# Detecção e risco

## Coordenadas visuais

Novas caixas usam `PDFBOX_ROTATED_CROP_TOP_LEFT`: origem no canto superior esquerdo do `CropBox`, largura e altura da página já ajustadas pela rotação intrínseca e posição Y no topo da caixa, não na linha de base. O extrator não converte pontos em pixels; o viewer escala proporcionalmente ao viewport real.

Ocorrências sem página, caixa completa ou evidência visual recebem `localizacaoConfiavel=false`. Históricos sem o novo metadado permanecem sem marcador até uma nova análise; o sistema não recalcula nem inventa coordenadas para apresentação.

O extrator estende `PDFTextStripper` para interceptar `TextPosition` e o estado gráfico. Ele registra página, crop box, rotação, caixa do texto, fonte, tamanho, cor de preenchimento, alpha e modo de renderização. Metadados de `PDDocumentInformation` também são extraídos. Uma propriedade não confiável permanece nula.

Regras:

| Código | Verificação |
|---|---|
| DS-001 | fonte abaixo de 2 pt |
| DS-002 | alpha abaixo de 5% ou modo sem fill/stroke; branco isolado não dispara |
| DS-003 | caixa de texto fora do crop box |
| DS-004 | controles, largura zero e marcas direcionais específicas |
| DS-005 | comandos imperativos de substituição/omissão de instruções em PT/EN |
| DS-006 | instruções em metadados |
| DS-007 | marcadores SYSTEM/DEVELOPER/ASSISTANT e equivalentes |
| DS-008 | pedidos de ocultação, desativação ou evasão de validações |

Texto é normalizado com Unicode NFKC, caixa baixa, remoção controlada de formatação e compactação de espaços. O motor não executa conteúdo.

A nota usa, no máximo, a ocorrência mais forte de cada regra: `round(pontuaçãoBase × confiança)`. Isso impede inflação por repetição. A coexistência de instrução para IA e mecanismo independente de ocultação adiciona 25 pontos. O total é limitado a 0–100: 0–9 informativo, 10–29 baixo, 30–49 médio, 50–79 alto, 80–100 crítico.

Limitações: texto convertido em imagem exige OCR, fora do MVP; cores de fundo complexas não são reconstruídas; clipping paths, grupos de transparência e conteúdo de formulários podem limitar a conclusão visual; `PDFTextStripper` não é um renderizador completo.
