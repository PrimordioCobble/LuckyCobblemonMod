package net.crulim.luckblockcobblemon.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ReadmeGenerator {

    public static void generateReadmeIfMissing() {
        File configDir = new File("config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File readme = new File(configDir, "README.txt");
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
                
                Config file: config/luckpocket_config.json
                
                Used by blocks like luck_block_pocket
                
                Supports events: item, cobbleitem, pokemon, shiny_pokemon, random_pokemon, structure
                
                You can modify drop chances, Pokémon level range, shiny chance, and structure spawning.
                
                2️⃣ Themed Lucky Blocks (By Pokémon Type):
                
                Each type has its own file, like:
                📁 config/luckblockcobblemon/luck_block_pocket_fire.json
                
                These blocks only spawn Pokémon and items related to that type (e.g., Fire-type).
                
                🔄 Reloading Configurations
                To apply changes without restarting Minecraft, use:
                /luckblock reload
                This command reloads all configuration files instantly without restarting the game.
                
                📌 Notes:
                
                All files must have valid JSON syntax, otherwise they will be ignored.
                
                Pokémon names must follow the Cobblemon format (e.g., charizard, mr_mime).
                
                If you want to spawn all Cobblemon Pokémon, ignoring the first 151:
                
                Increase the chance of the RandomCobblemon group
                
                Set the SpawnCobblemon group chance to 0
                
                ==============================
                
                🌎 PORTUGUÊS
                Existem dois tipos de Lucky Blocks neste mod:
                
                1️⃣ Lucky Block Padrão:
                
                Arquivo de config: config/luckpocket_config.json
                
                Usado por blocos como luck_block_pocket
                
                Suporta eventos: item, cobbleitem, pokemon, shiny_pokemon, random_pokemon, structure
                
                Você pode modificar probabilidades de drop, nível dos Pokémon, chance de shiny e estruturas.
                
                2️⃣ Lucky Blocks Temáticos (Por Tipo de Pokémon):
                
                Cada tipo tem seu próprio arquivo, como:
                📁 config/luckblockcobblemon/luck_block_pocket_fire.json
                
                Esses blocos só gerarão Pokémon e itens relacionados ao tipo (ex: tipo Fogo).
                
                🔄 Recarregar Configurações
                Para aplicar mudanças sem fechar o Minecraft, use:
                /luckblock reload
                Esse comando recarrega todos os arquivos de configuração imediatamente, sem reiniciar o jogo.
                
                📌 Observações:
                
                Todos os arquivos devem estar com sintaxe JSON válida, ou serão ignorados.
                
                Os nomes dos Pokémon devem seguir o padrão do Cobblemon (ex: charizard, mr_mime).
                
                Se quiser que o bloco gere todos os Pokémon do Cobblemon, ignore a lista dos 151 iniciais:
                
                Aumente a chance do grupo RandomCobblemon
                
                Zere a chance do grupo SpawnCobblemon
                
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