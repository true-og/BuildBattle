![](https://images.plugily.xyz/banner/display.php?id=BuildBattle-OG)

# BuildBattle-OG / Guess The Build [![Maven Repository](https://maven.plugily.xyz/api/badge/latest/releases/plugily/projects/buildbattle?color=40c14a&name=Maven&prefix=v)](https://maven.plugily.xyz/#/releases/plugily/projects/buildbattle) [![JavaDoc Repository](https://maven.plugily.xyz/api/badge/latest/releases/plugily/projects/buildbattle?color=40c14a&name=JavaDoc&prefix=v)](https://maven.plugily.xyz/javadoc/releases/plugily/projects/buildbattle/latest) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Plugily-Projects_BuildBattle&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Plugily-Projects_BuildBattle) [![Discord](https://img.shields.io/discord/345628548716822530.svg?color=7289DA&style=for-the-badge&logo=discord)](https://discord.plugily.xyz) [![Patreon](    https://img.shields.io/badge/Patreon-F96854?style=for-the-badge&logo=patreon&logoColor=white)](https://patreon.com/plugily)

BuildBattle-OG / Guess The Build is a Minecraft minigame designed for small and big servers. This minigame is unique and very configurable,
100% free and open source!

## OG fork notes

This fork has no outbound phone-home. Upstream Plugily service hooks (`api.plugily.xyz` ping, remote locale fetching, automatic error reporting) and the Spiget-backed update checker are stubbed out at build time via local class overrides. Bundled `Default` locale is always used; update notifications are disabled. bStats metrics are untouched. See [CHANGELOG.md](CHANGELOG.md) for details.

There are different modes such as the classic BuildBattle-OG and the guess mode called Guess The Build. On the classic version the goal is to build the best you can on your own as solo or as team with unlimited team sizes! You must compete with other players in this building game. Who will be the best? On the guess mode you have to build whileas the other players are guessing the correct word according to your building which grants points to the players. The player with the most points (best guesses) wins. 
Have fun using it! Leave a good rating if you really like it.

## Want to contribute in this project?

[**💣 Issues Reporting (Discord)**](https://discordapp.com/invite/UXzUdTP)
[**❤ Make Donation**](https://www.paypal.me/plugilyprojects)

# Credits

## Open Source Libraries

| Library                                                     | Author                                                | License                                                                            |
|-------------------------------------------------------------|-------------------------------------------------------|------------------------------------------------------------------------------------|
| [InventoryFramework](https://github.com/stefvanschie/IF/)   | [stefvanschie](https://github.com/stefvanschie)       | [Unlicense](https://github.com/stefvanschie/IF/blob/master/LICENSE)                |
| [ScoreboardLib](https://github.com/TigerHix/ScoreboardLib/) | [TigerHix](https://github.com/TigerHix)               | [LGPLv3](https://github.com/TigerHix/ScoreboardLib/blob/master/LICENSE)            |
| [HikariCP](https://github.com/brettwooldridge/HikariCP)     | [brettwooldridge](https://github.com/brettwooldridge) | [Apache License 2.0](https://github.com/brettwooldridge/HikariCP/blob/dev/LICENSE) |
| [bStats](https://github.com/Bastian/bStats-Metrics)         | [Bastian](https://github.com/Bastian)                 | [LGPLv3](https://github.com/Bastian/bStats-Metrics/blob/master/LICENSE)            |
| [Commons Box](https://github.com/Plajer/Commons-Box)        | [Plajer](https://github.com/Plajer)                   | [GPLv3](https://github.com/Plajer/Commons-Box/blob/master/LICENSE.md)              |

## Open Source Licenses

#### Code Whale

<img src="https://poeditor.com/public/images/logo/logo_head_500_transparent.png" alt="jetbrains logo" width="150"/>

Thanks to Code Whale for Open Source license for POEditor project, so we are able to have locales.

#### Minecraft Heads

[![https://minecraft-heads.com/](https://images.minecraft-heads.com/banners/minecraft-heads_leaderboard_728x90.png)](https://minecraft-heads.com/)

Thanks to Minecraft Heads to let us use the Name and Textures of the heads to provide you a better heads experience ingame.

## Contributors

<a href="https://github.com/Plugily-Projects/BuildBattle/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Plugily-Projects/BuildBattle" />
</a>

<img align="right" src="https://i.imgur.com/EmFfDXN.png">

