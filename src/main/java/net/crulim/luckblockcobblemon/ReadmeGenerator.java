package net.crulim.luckblockcobblemon.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ReadmeGenerator {

    public static void generateReadmeIfMissing() {
        File configDir = new File("config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File readme = new File(configDir, "README LUCK COBBLEMON.txt");
        if (readme.exists()) {
            return;
        }

        String content = """
                ==============================
                🎲 LUCKY BLOCK CONFIGURATION
                ==============================
                
                🌍 ENGLISH
                There are two types of Lucky Blocks in this mod:
                
                1️⃣ Standard Lucky Block:
                
                Config file: config/luckpocket_config.json \s
                Used by blocks like `luck_block_pocket`
                
                Supports events: `item`, `cobbleitem`, `pokemon`, `shiny_pokemon`, `random_pokemon`, `multi_pokemon`, `structure`, `cobblemon_allitems`
                
                You can modify drop chances, Pokémon spawn levels (using global weighting or per-event level range), shiny chance, and structure spawning.
                
                2️⃣ Themed Lucky Blocks (By Pokémon Type):
                
                Each type has its own file, like: \s
                📁 config/luckblockcobblemon/luck_block_pocket_fire.json
                
                These blocks only spawn Pokémon and items related to that type (e.g., Fire-type).
                
                🔄 Reloading Configurations \s
                To apply changes without restarting Minecraft, use: \s
                `/luckblock reload` \s
                This command reloads all configuration files instantly.
                
                📌 Notes:
                
                - All files must have valid JSON syntax, or they will be ignored. \s
                - Pokémon names must follow the Cobblemon format (e.g., `charizard`, `mr_mime`). \s
                - If you want to spawn all Cobblemon Pokémon and skip the Gen 1 list: \s
                  - Increase the chance of `random_cobblemonp` \s
                  - Set the chance of `cobblemonp` to 0
                
                📌 Level Handling:
                - You can define `"minLevel"` and `"maxLevel"` inside each Pokémon event (`cobblemonp`, `random_cobblemonp`, `multi_cobblemonp`, etc.) to customize level range per event.
                - If omitted, the system uses `"levelWeighting"` to determine levels.
                - If neither is defined, the default fallback is level 5–30.
                - `"levelRange"` is deprecated and only used when `"levelWeighting"` is not defined.
                
                📌 Shiny Chance:
                - Each Pokémon event can override the shiny chance using `"shinyChance"`. \s
                - If not defined, the global `"shinyChancePercent"` (default: `0.02`) will be used.
                
                ==============================
                
                🌎 PORTUGUÊS \s
                Existem dois tipos de Lucky Blocks neste mod:
                
                1️⃣ Lucky Block Padrão:
                
                Arquivo de configuração: `config/luckpocket_config.json` \s
                Usado por blocos como `luck_block_pocket`
                
                Suporta eventos: `item`, `cobbleitem`, `pokemon`, `shiny_pokemon`, `random_pokemon`, `multi_pokemon`, `structure`, `cobblemon_allitems`
                
                Você pode modificar as chances de drop, os níveis de Pokémon (usando pesos globais ou intervalo por evento), chance de shiny e estruturas.
                
                2️⃣ Lucky Blocks Temáticos (por Tipo de Pokémon):
                
                Cada tipo possui seu próprio arquivo, como: \s
                📁 config/luckblockcobblemon/luck_block_pocket_fire.json
                
                Esses blocos só gerarão Pokémon e itens relacionados ao tipo (ex: tipo Fogo).
                
                🔄 Recarregando Configurações \s
                Para aplicar mudanças sem fechar o Minecraft, use: \s
                `/luckblock reload` \s
                Esse comando recarrega todos os arquivos imediatamente.
                
                📌 Observações:
                
                - Todos os arquivos precisam estar com sintaxe JSON válida, ou serão ignorados. \s
                - Os nomes dos Pokémon devem seguir o padrão Cobblemon (ex: `charizard`, `mr_mime`). \s
                - Se quiser que o bloco gere todos os Pokémon do Cobblemon e ignore os 151 iniciais: \s
                  - Aumente a chance de `random_cobblemonp` \s
                  - Zere a chance de `cobblemonp`
                
                📌 Controle de Nível:
                - Você pode definir `"minLevel"` e `"maxLevel"` dentro de cada evento Pokémon (`cobblemonp`, `random_cobblemonp`, `multi_cobblemonp`, etc.) para personalizar o nível individualmente.
                - Se esses campos não estiverem presentes, será usado o `"levelWeighting"` para determinar o nível.
                - Se nenhum for definido, o sistema usará o intervalo padrão de nível 5–30.
                - `"levelRange"` é obsoleto e só será usado se não houver `"levelWeighting"`.
                
                📌 Chance de Shiny:
                - Cada evento pode definir `"shinyChance"` individualmente.
                - Se não estiver presente, será usada a chance global `"shinyChancePercent"` (padrão: `0.02`).
                
                ==============================
                
        """;

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(readme), StandardCharsets.UTF_8)) {
            writer.write(content);
            System.out.println("[LuckyBlockPocket] README.txt generated in config/luckblockcobblemon.");
        } catch (IOException e) {
            System.out.println("[LuckyBlockPocket] Failed to generate README.txt: " + e.getMessage());
        }
    }
}