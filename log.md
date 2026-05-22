unexpected character found
(10)
 in 'reader', line 16, column 12:
      target: *
               ^

        at org.yaml.snakeyaml.scanner.ScannerImpl.scanAnchor(ScannerImpl.java:1503) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.scanner.ScannerImpl.fetchAlias(ScannerImpl.java:947) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.scanner.ScannerImpl.fetchMoreTokens(ScannerImpl.java:397) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.scanner.ScannerImpl.checkToken(ScannerImpl.java:238) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.parser.ParserImpl$ParseBlockMappingValue.produce(ParserImpl.java:669) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.parser.ParserImpl.peekEvent(ParserImpl.java:161) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector$1.peek(CommentEventsCollector.java:57) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector$1.peek(CommentEventsCollector.java:43) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector.collectEvents(CommentEventsCollector.java:136) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector.collectEvents(CommentEventsCollector.java:116) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeScalarNode(Composer.java:241) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeNode(Composer.java:205) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeKeyNode(Composer.java:359) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingChildren(Composer.java:344) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingNode(Composer.java:323) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeNode(Composer.java:209) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeValueNode(Composer.java:369) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingChildren(Composer.java:348) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingNode(Composer.java:323) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeNode(Composer.java:209) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.getNode(Composer.java:131) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.getSingleNode(Composer.java:157) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.Yaml.compose(Yaml.java:575) ~[snakeyaml-2.2.jar:?]
        at org.bukkit.configuration.file.YamlConfiguration.loadFromString(YamlConfiguration.java:105) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        ... 16 more
[04:57:33 WARN]: [dPlayerProfiles] Achievement in truhly_3.yml is missing 'id' — skipping.
[04:57:33 ERROR]: Cannot load plugins/dPlayerProfiles/achievements/truhly_4.yml
org.bukkit.configuration.InvalidConfigurationException: while scanning an alias
 in 'reader', line 16, column 11:
      target: *
              ^
unexpected character found
(10)
 in 'reader', line 16, column 12:
      target: *
               ^

        at org.bukkit.configuration.file.YamlConfiguration.loadFromString(YamlConfiguration.java:112) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at org.bukkit.configuration.file.FileConfiguration.load(FileConfiguration.java:160) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at org.bukkit.configuration.file.FileConfiguration.load(FileConfiguration.java:128) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(YamlConfiguration.java:310) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at dPlayerProfiles-1.0.0.jar//com.dogetennant.dplayerprofiles.config.AchievementConfigLoader.load(AchievementConfigLoader.java:46) ~[?:?]
        at dPlayerProfiles-1.0.0.jar//com.dogetennant.dplayerprofiles.DPlayerProfiles.onEnable(DPlayerProfiles.java:80) ~[?:?]
        at org.bukkit.plugin.java.JavaPlugin.setEnabled(JavaPlugin.java:279) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at io.papermc.paper.plugin.manager.PaperPluginInstanceManager.enablePlugin(PaperPluginInstanceManager.java:207) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at io.papermc.paper.plugin.manager.PaperPluginManagerImpl.enablePlugin(PaperPluginManagerImpl.java:109) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at org.bukkit.plugin.SimplePluginManager.enablePlugin(SimplePluginManager.java:520) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at org.bukkit.craftbukkit.CraftServer.enablePlugin(CraftServer.java:637) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at org.bukkit.craftbukkit.CraftServer.enablePlugins(CraftServer.java:594) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at net.minecraft.server.MinecraftServer.initPostWorld(MinecraftServer.java:680) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at net.minecraft.server.dedicated.DedicatedServer.initServer(DedicatedServer.java:386) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at net.minecraft.server.MinecraftServer.runServer(MinecraftServer.java:1290) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at net.minecraft.server.MinecraftServer.lambda$spin$0(MinecraftServer.java:304) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at java.base/java.lang.Thread.run(Unknown Source) ~[?:?]
Caused by: org.yaml.snakeyaml.scanner.ScannerException: while scanning an alias
 in 'reader', line 16, column 11:
      target: *
              ^
unexpected character found
(10)
 in 'reader', line 16, column 12:
      target: *
               ^

        at org.yaml.snakeyaml.scanner.ScannerImpl.scanAnchor(ScannerImpl.java:1503) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.scanner.ScannerImpl.fetchAlias(ScannerImpl.java:947) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.scanner.ScannerImpl.fetchMoreTokens(ScannerImpl.java:397) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.scanner.ScannerImpl.checkToken(ScannerImpl.java:238) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.parser.ParserImpl$ParseBlockMappingValue.produce(ParserImpl.java:669) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.parser.ParserImpl.peekEvent(ParserImpl.java:161) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector$1.peek(CommentEventsCollector.java:57) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector$1.peek(CommentEventsCollector.java:43) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector.collectEvents(CommentEventsCollector.java:136) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector.collectEvents(CommentEventsCollector.java:116) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeScalarNode(Composer.java:241) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeNode(Composer.java:205) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeKeyNode(Composer.java:359) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingChildren(Composer.java:344) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingNode(Composer.java:323) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeNode(Composer.java:209) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeValueNode(Composer.java:369) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingChildren(Composer.java:348) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingNode(Composer.java:323) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeNode(Composer.java:209) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.getNode(Composer.java:131) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.getSingleNode(Composer.java:157) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.Yaml.compose(Yaml.java:575) ~[snakeyaml-2.2.jar:?]
        at org.bukkit.configuration.file.YamlConfiguration.loadFromString(YamlConfiguration.java:105) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        ... 16 more
[04:57:33 WARN]: [dPlayerProfiles] Achievement in truhly_4.yml is missing 'id' — skipping.
[04:57:33 ERROR]: Cannot load plugins/dPlayerProfiles/achievements/turnaje_1.yml
org.bukkit.configuration.InvalidConfigurationException: while scanning an alias
 in 'reader', line 16, column 11:
      target: *
              ^
unexpected character found
(10)
 in 'reader', line 16, column 12:
      target: *
               ^

        at org.bukkit.configuration.file.YamlConfiguration.loadFromString(YamlConfiguration.java:112) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at org.bukkit.configuration.file.FileConfiguration.load(FileConfiguration.java:160) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at org.bukkit.configuration.file.FileConfiguration.load(FileConfiguration.java:128) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(YamlConfiguration.java:310) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at dPlayerProfiles-1.0.0.jar//com.dogetennant.dplayerprofiles.config.AchievementConfigLoader.load(AchievementConfigLoader.java:46) ~[?:?]
        at dPlayerProfiles-1.0.0.jar//com.dogetennant.dplayerprofiles.DPlayerProfiles.onEnable(DPlayerProfiles.java:80) ~[?:?]
        at org.bukkit.plugin.java.JavaPlugin.setEnabled(JavaPlugin.java:279) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at io.papermc.paper.plugin.manager.PaperPluginInstanceManager.enablePlugin(PaperPluginInstanceManager.java:207) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at io.papermc.paper.plugin.manager.PaperPluginManagerImpl.enablePlugin(PaperPluginManagerImpl.java:109) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at org.bukkit.plugin.SimplePluginManager.enablePlugin(SimplePluginManager.java:520) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at org.bukkit.craftbukkit.CraftServer.enablePlugin(CraftServer.java:637) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at org.bukkit.craftbukkit.CraftServer.enablePlugins(CraftServer.java:594) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at net.minecraft.server.MinecraftServer.initPostWorld(MinecraftServer.java:680) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at net.minecraft.server.dedicated.DedicatedServer.initServer(DedicatedServer.java:386) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at net.minecraft.server.MinecraftServer.runServer(MinecraftServer.java:1290) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at net.minecraft.server.MinecraftServer.lambda$spin$0(MinecraftServer.java:304) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at java.base/java.lang.Thread.run(Unknown Source) ~[?:?]
Caused by: org.yaml.snakeyaml.scanner.ScannerException: while scanning an alias
 in 'reader', line 16, column 11:
      target: *
              ^
unexpected character found
(10)
 in 'reader', line 16, column 12:
      target: *
               ^

        at org.yaml.snakeyaml.scanner.ScannerImpl.scanAnchor(ScannerImpl.java:1503) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.scanner.ScannerImpl.fetchAlias(ScannerImpl.java:947) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.scanner.ScannerImpl.fetchMoreTokens(ScannerImpl.java:397) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.scanner.ScannerImpl.checkToken(ScannerImpl.java:238) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.parser.ParserImpl$ParseBlockMappingValue.produce(ParserImpl.java:669) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.parser.ParserImpl.peekEvent(ParserImpl.java:161) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector$1.peek(CommentEventsCollector.java:57) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector$1.peek(CommentEventsCollector.java:43) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector.collectEvents(CommentEventsCollector.java:136) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector.collectEvents(CommentEventsCollector.java:116) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeScalarNode(Composer.java:241) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeNode(Composer.java:205) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeKeyNode(Composer.java:359) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingChildren(Composer.java:344) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingNode(Composer.java:323) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeNode(Composer.java:209) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeValueNode(Composer.java:369) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingChildren(Composer.java:348) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingNode(Composer.java:323) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeNode(Composer.java:209) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.getNode(Composer.java:131) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.getSingleNode(Composer.java:157) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.Yaml.compose(Yaml.java:575) ~[snakeyaml-2.2.jar:?]
        at org.bukkit.configuration.file.YamlConfiguration.loadFromString(YamlConfiguration.java:105) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        ... 16 more
[04:57:33 WARN]: [dPlayerProfiles] Achievement in turnaje_1.yml is missing 'id' — skipping.
[04:57:33 ERROR]: Cannot load plugins/dPlayerProfiles/achievements/turnaje_25.yml
org.bukkit.configuration.InvalidConfigurationException: while scanning an alias
 in 'reader', line 16, column 11:
      target: *
              ^
unexpected character found
(10)
 in 'reader', line 16, column 12:
      target: *
               ^

        at org.bukkit.configuration.file.YamlConfiguration.loadFromString(YamlConfiguration.java:112) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at org.bukkit.configuration.file.FileConfiguration.load(FileConfiguration.java:160) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at org.bukkit.configuration.file.FileConfiguration.load(FileConfiguration.java:128) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(YamlConfiguration.java:310) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at dPlayerProfiles-1.0.0.jar//com.dogetennant.dplayerprofiles.config.AchievementConfigLoader.load(AchievementConfigLoader.java:46) ~[?:?]
        at dPlayerProfiles-1.0.0.jar//com.dogetennant.dplayerprofiles.DPlayerProfiles.onEnable(DPlayerProfiles.java:80) ~[?:?]
        at org.bukkit.plugin.java.JavaPlugin.setEnabled(JavaPlugin.java:279) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at io.papermc.paper.plugin.manager.PaperPluginInstanceManager.enablePlugin(PaperPluginInstanceManager.java:207) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at io.papermc.paper.plugin.manager.PaperPluginManagerImpl.enablePlugin(PaperPluginManagerImpl.java:109) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at org.bukkit.plugin.SimplePluginManager.enablePlugin(SimplePluginManager.java:520) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at org.bukkit.craftbukkit.CraftServer.enablePlugin(CraftServer.java:637) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at org.bukkit.craftbukkit.CraftServer.enablePlugins(CraftServer.java:594) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at net.minecraft.server.MinecraftServer.initPostWorld(MinecraftServer.java:680) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at net.minecraft.server.dedicated.DedicatedServer.initServer(DedicatedServer.java:386) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at net.minecraft.server.MinecraftServer.runServer(MinecraftServer.java:1290) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at net.minecraft.server.MinecraftServer.lambda$spin$0(MinecraftServer.java:304) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at java.base/java.lang.Thread.run(Unknown Source) ~[?:?]
Caused by: org.yaml.snakeyaml.scanner.ScannerException: while scanning an alias
 in 'reader', line 16, column 11:
      target: *
              ^
unexpected character found
(10)
 in 'reader', line 16, column 12:
      target: *
               ^

        at org.yaml.snakeyaml.scanner.ScannerImpl.scanAnchor(ScannerImpl.java:1503) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.scanner.ScannerImpl.fetchAlias(ScannerImpl.java:947) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.scanner.ScannerImpl.fetchMoreTokens(ScannerImpl.java:397) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.scanner.ScannerImpl.checkToken(ScannerImpl.java:238) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.parser.ParserImpl$ParseBlockMappingValue.produce(ParserImpl.java:669) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.parser.ParserImpl.peekEvent(ParserImpl.java:161) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector$1.peek(CommentEventsCollector.java:57) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector$1.peek(CommentEventsCollector.java:43) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector.collectEvents(CommentEventsCollector.java:136) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector.collectEvents(CommentEventsCollector.java:116) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeScalarNode(Composer.java:241) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeNode(Composer.java:205) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeKeyNode(Composer.java:359) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingChildren(Composer.java:344) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingNode(Composer.java:323) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeNode(Composer.java:209) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeValueNode(Composer.java:369) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingChildren(Composer.java:348) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingNode(Composer.java:323) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeNode(Composer.java:209) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.getNode(Composer.java:131) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.getSingleNode(Composer.java:157) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.Yaml.compose(Yaml.java:575) ~[snakeyaml-2.2.jar:?]
        at org.bukkit.configuration.file.YamlConfiguration.loadFromString(YamlConfiguration.java:105) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        ... 16 more
[04:57:33 WARN]: [dPlayerProfiles] Achievement in turnaje_25.yml is missing 'id' — skipping.
[04:57:33 ERROR]: Cannot load plugins/dPlayerProfiles/achievements/turnaje_5.yml
org.bukkit.configuration.InvalidConfigurationException: while scanning an alias
 in 'reader', line 16, column 11:
      target: *
              ^
unexpected character found
(10)
 in 'reader', line 16, column 12:
      target: *
               ^

        at org.bukkit.configuration.file.YamlConfiguration.loadFromString(YamlConfiguration.java:112) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at org.bukkit.configuration.file.FileConfiguration.load(FileConfiguration.java:160) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at org.bukkit.configuration.file.FileConfiguration.load(FileConfiguration.java:128) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(YamlConfiguration.java:310) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at dPlayerProfiles-1.0.0.jar//com.dogetennant.dplayerprofiles.config.AchievementConfigLoader.load(AchievementConfigLoader.java:46) ~[?:?]
        at dPlayerProfiles-1.0.0.jar//com.dogetennant.dplayerprofiles.DPlayerProfiles.onEnable(DPlayerProfiles.java:80) ~[?:?]
        at org.bukkit.plugin.java.JavaPlugin.setEnabled(JavaPlugin.java:279) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at io.papermc.paper.plugin.manager.PaperPluginInstanceManager.enablePlugin(PaperPluginInstanceManager.java:207) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at io.papermc.paper.plugin.manager.PaperPluginManagerImpl.enablePlugin(PaperPluginManagerImpl.java:109) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at org.bukkit.plugin.SimplePluginManager.enablePlugin(SimplePluginManager.java:520) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        at org.bukkit.craftbukkit.CraftServer.enablePlugin(CraftServer.java:637) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at org.bukkit.craftbukkit.CraftServer.enablePlugins(CraftServer.java:594) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at net.minecraft.server.MinecraftServer.initPostWorld(MinecraftServer.java:680) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at net.minecraft.server.dedicated.DedicatedServer.initServer(DedicatedServer.java:386) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at net.minecraft.server.MinecraftServer.runServer(MinecraftServer.java:1290) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at net.minecraft.server.MinecraftServer.lambda$spin$0(MinecraftServer.java:304) ~[paper-26.1.1.jar:26.1.1-29-77c0866]
        at java.base/java.lang.Thread.run(Unknown Source) ~[?:?]
Caused by: org.yaml.snakeyaml.scanner.ScannerException: while scanning an alias
 in 'reader', line 16, column 11:
      target: *
              ^
unexpected character found
(10)
 in 'reader', line 16, column 12:
      target: *
               ^

        at org.yaml.snakeyaml.scanner.ScannerImpl.scanAnchor(ScannerImpl.java:1503) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.scanner.ScannerImpl.fetchAlias(ScannerImpl.java:947) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.scanner.ScannerImpl.fetchMoreTokens(ScannerImpl.java:397) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.scanner.ScannerImpl.checkToken(ScannerImpl.java:238) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.parser.ParserImpl$ParseBlockMappingValue.produce(ParserImpl.java:669) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.parser.ParserImpl.peekEvent(ParserImpl.java:161) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector$1.peek(CommentEventsCollector.java:57) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector$1.peek(CommentEventsCollector.java:43) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector.collectEvents(CommentEventsCollector.java:136) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.comments.CommentEventsCollector.collectEvents(CommentEventsCollector.java:116) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeScalarNode(Composer.java:241) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeNode(Composer.java:205) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeKeyNode(Composer.java:359) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingChildren(Composer.java:344) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingNode(Composer.java:323) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeNode(Composer.java:209) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeValueNode(Composer.java:369) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingChildren(Composer.java:348) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeMappingNode(Composer.java:323) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.composeNode(Composer.java:209) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.getNode(Composer.java:131) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.composer.Composer.getSingleNode(Composer.java:157) ~[snakeyaml-2.2.jar:?]
        at org.yaml.snakeyaml.Yaml.compose(Yaml.java:575) ~[snakeyaml-2.2.jar:?]
        at org.bukkit.configuration.file.YamlConfiguration.loadFromString(YamlConfiguration.java:105) ~[paper-api-26.1.1.build.29-alpha.jar:?]
        ... 16 more