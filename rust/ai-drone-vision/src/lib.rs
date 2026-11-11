//! CPU post-processing of single-class YOLO person detections.
//! The output layout is five contiguous rows [1, 5, N] of float32 values:
//! cx, cy, width, height, score. No GPU/TFLite runtime is bundled.
//!
//! This module intentionally does not run the aircraft or control flight.

use std::cmp::Ordering;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CoordinateFormat {
    /// Values already normalized to the unit square (LiteRT-Torch YOLO26).
    Normalized,
    /// Pixel values relative to the *model input* dimensions.
    Pixels { width: u32, height: u32 },
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct Detection {
    pub left: f32,
    pub top: f32,
    pub right: f32,
    pub bottom: f32,
    pub confidence: f32,
}

#[derive(Debug, Clone, Copy)]
pub struct Config {
    pub confidence_threshold: f32,
    pub iou_threshold: f32,
    pub max_detections: usize,
    pub min_box_side: f32,
}

impl Default for Config {
    fn default() -> Self {
        Self {
            confidence_threshold: 0.27,
            iou_threshold: 0.45,
            max_detections: 20,
            min_box_side: 0.005,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum DecodeError {
    InvalidShape,
    InvalidCoordinateDimensions,
    InvalidConfig,
}

impl Detection {
    fn iou(&self, other: &Self) -> f32 {
        let left = self.left.max(other.left);
        let right = self.right.min(other.right);
        let top = self.top.max(other.top);
        let bottom = self.bottom.min(other.bottom);
        let intersection = (right - left).max(0.0) * (bottom - top).max(0.0);
        let a = (self.right - self.left).max(0.0) * (self.bottom - self.top).max(0.0);
        let b = (other.right - other.left).max(0.0) * (other.bottom - other.top).max(0.0);
        let union = a + b - intersection;
        if union <= 0.0 { 0.0 } else { intersection / union }
    }
}

/// Decode one-class YOLO output, apply a confidence threshold, and use NMS.
///
/// Coordinate format must be explicitly provided based on the exported model's
/// validated metadata/output; do not guess per-frame from the boxes.
/// Output coordinates are clamped to [0, 1] for the Compose overlay.
pub fn decode_people(
    output: &[f32],
    prediction_count: usize,
    coordinates: CoordinateFormat,
    config: Config,
) -> Result<Vec<Detection>, DecodeError> {
    if prediction_count == 0
        || output.len() != prediction_count.checked_mul(5).ok_or(DecodeError::InvalidShape)?
    {
        return Err(DecodeError::InvalidShape);
    }
    let (sx, sy) = match coordinates {
        CoordinateFormat::Normalized => (1.0, 1.0),
        CoordinateFormat::Pixels { width, height } if width != 0 && height != 0 => {
            (width as f32, height as f32)
        }
        _ => return Err(DecodeError::InvalidCoordinateDimensions),
    };
    if !config.confidence_threshold.is_finite()
        || !(0.0..=1.0).contains(&config.confidence_threshold)
        || !config.iou_threshold.is_finite()
        || !(0.0..=1.0).contains(&config.iou_threshold)
        || !config.min_box_side.is_finite()
        || config.min_box_side < 0.0
    {
        return Err(DecodeError::InvalidConfig);
    }

    let mut candidates = Vec::new();
    for i in 0..prediction_count {
        let score = output[4 * prediction_count + i];
        if !score.is_finite() || score < config.confidence_threshold || score > 1.0 {
            continue;
        }
        let cx = output[i] / sx;
        let cy = output[prediction_count + i] / sy;
        let w = output[2 * prediction_count + i] / sx;
        let h = output[3 * prediction_count + i] / sy;
        if !cx.is_finite() || !cy.is_finite() || !w.is_finite() || !h.is_finite()
            || w <= 0.0 || h <= 0.0
        {
            continue;
        }
        let left = (cx - w / 2.0).clamp(0.0, 1.0);
        let top = (cy - h / 2.0).clamp(0.0, 1.0);
        let right = (cx + w / 2.0).clamp(0.0, 1.0);
        let bottom = (cy + h / 2.0).clamp(0.0, 1.0);
        if right - left < config.min_box_side || bottom - top < config.min_box_side {
            continue;
        }
        candidates.push(Detection { left, top, right, bottom, confidence: score });
    }

    candidates.sort_by(|a, b| {
        b.confidence.partial_cmp(&a.confidence).unwrap_or(Ordering::Equal)
    });
    let mut selected = Vec::with_capacity(config.max_detections.min(candidates.len()));
    for candidate in candidates {
        if selected.len() >= config.max_detections {
            break;
        }
        if selected.iter().all(|previous: &Detection| previous.iou(&candidate) <= config.iou_threshold) {
            selected.push(candidate);
        }
    }
    Ok(selected)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn rows(boxes: &[[f32; 5]]) -> Vec<f32> {
        let mut tensor = vec![0.0; 5 * boxes.len()];
        for (i, bbox) in boxes.iter().enumerate() {
            for row in 0..5 {
                tensor[row * boxes.len() + i] = bbox[row];
            }
        }
        tensor
    }

    #[test]
    fn normalized_yolo26_values_are_not_divided_by_640() {
        // Realistic scale from pessoas_640.tflite output inspected on VisDrone.
        let raw = rows(&[[0.5128692, 0.7480704, 0.01462603, 0.0269908, 0.47870317]]);
        let out = decode_people(&raw, 1, CoordinateFormat::Normalized, Config::default()).unwrap();
        assert_eq!(out.len(), 1);
        assert!(out[0].left > 0.50 && out[0].right < 0.53);
        assert!(out[0].confidence > 0.47);
    }

    #[test]
    fn pixel_and_normalized_forms_match() {
        let normalized = rows(&[[0.5, 0.5, 0.1, 0.2, 0.9]]);
        let pixels = rows(&[[320.0, 320.0, 64.0, 128.0, 0.9]]);
        let a = decode_people(&normalized, 1, CoordinateFormat::Normalized, Config::default()).unwrap();
        let b = decode_people(&pixels, 1, CoordinateFormat::Pixels { width: 640, height: 640 }, Config::default()).unwrap();
        assert_eq!(a, b);
    }

    #[test]
    fn removes_duplicates_but_keeps_neighboring_people() {
        let raw = rows(&[
            [0.2, 0.5, 0.1, 0.2, 0.9],
            [0.201, 0.5, 0.1, 0.2, 0.8],
            [0.6, 0.5, 0.1, 0.2, 0.7],
        ]);
        let out = decode_people(&raw, 3, CoordinateFormat::Normalized, Config::default()).unwrap();
        assert_eq!(out.len(), 2);
        assert_eq!(out[0].confidence, 0.9);
        assert_eq!(out[1].confidence, 0.7);
    }

    #[test]
    fn rejects_nan_and_tiny_boxes() {
        let raw = rows(&[
            [f32::NAN, 0.5, 0.1, 0.2, 0.9],
            [0.5, 0.5, 0.0001, 0.2, 0.9],
            [0.5, 0.5, 0.1, 0.2, f32::NAN],
        ]);
        let out = decode_people(&raw, 3, CoordinateFormat::Normalized, Config::default()).unwrap();
        assert!(out.is_empty());
    }

    #[test]
    fn validates_shape_and_dimensions() {
        assert_eq!(decode_people(&[], 1, CoordinateFormat::Normalized, Config::default()), Err(DecodeError::InvalidShape));
        let raw = rows(&[[0.5, 0.5, 0.1, 0.2, 0.9]]);
        assert_eq!(decode_people(&raw, 1, CoordinateFormat::Pixels { width: 0, height: 640 }, Config::default()), Err(DecodeError::InvalidCoordinateDimensions));
    }
}
