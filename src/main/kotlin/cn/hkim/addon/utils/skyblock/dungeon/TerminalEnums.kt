package cn.hkim.addon.utils.skyblock.dungeon

enum class TerminalEnums(val regex: Regex){
    PANES(Regex("^Correct all the panes!$")),
    RUBIX(Regex("^Change all to same color!$")),
    NUMBERS(Regex("^Click in order!$")),
    START_WITH(Regex("^What starts with: '(\\w)'\\?$")),
    SELECT(Regex("^Select all the ([\\w ]+) items!$")),
    MELODY(Regex("^Click the button on time!$"))
}