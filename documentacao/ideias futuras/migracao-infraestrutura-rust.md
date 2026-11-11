# Ideia futura: migrar infraestrutura para Rust

**Status:** ideia futura, sem implementação planejada para a fase atual.

## Objetivo

Avaliar a migração gradual de componentes de infraestrutura e processamento do projeto ai-drone para Rust, visando segurança de memória, previsibilidade de desempenho e reutilização entre plataformas.

## Possíveis áreas de estudo

- Núcleo de processamento de imagens e pré/pós-processamento da detecção, quando medições justificarem.
- Pipeline de telemetria, mensagens e armazenamento local.
- Componentes compartilhados entre Android e ferramentas de desktop.
- Integração via JNI/FFI com a aplicação Android em Kotlin, mantendo uma interface estável.

## Cuidados e pré-requisitos

- **Não migrar diretamente o DJI Mobile SDK 4.18:** trata-se de dependência proprietária Android, e uma migração para Rust não elimina as limitações de compatibilidade de suas bibliotecas nativas.
- Medir desempenho, latência, memória, custo de manutenção e tamanho do APK antes de escolher componentes.
- Verificar compatibilidade com arquiteturas Android, toolchains, bibliotecas nativas e páginas de memória de 16 KB.
- Manter o aplicativo funcional durante uma eventual migração por módulos, com testes automatizados e possibilidade de reversão.
- Preservar a separação entre detecção de pessoas e funções de voo; nenhuma automação de voo deve resultar desta proposta por padrão.

## Critério para iniciar

Reavaliar após estabilizar a conexão com o Mavic Pro, a captura de vídeo e a inferência offline no Galaxy Tab S9. Priorizar Rust apenas onde houver benefício mensurável sobre a solução Kotlin/Android atual.
