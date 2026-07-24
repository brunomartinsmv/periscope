# Dataset de teste

Conjunto de referência para validação local (importação, harmonização, relatórios).

## Conteúdo atual

Placeholder — incluir arquivos de patentes de exemplo (PDF/XML/CSV conforme importadores) em commits posteriores da implementação.

## Critérios do dataset

- Poucos registros (rápido de importar)
- Casos com nomes similares para Fast-Join (depositantes/inventores)
- Ao menos um caso multi-formato se o importador permitir

## Uso

1. Subir MongoDB limpo ou DB de teste
2. Login `admin` / `123456` (seed)
3. Criar projeto de teste
4. Importar arquivos deste diretório
5. Executar harmonização e um relatório fixo
