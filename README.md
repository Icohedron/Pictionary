# Pictionary
Pictionary plugin for the EGO Minecraft event server.  

## Permissions
```
pictionary.command.set.entity
pictionary.command.set.string
pictionary.command.clear
pictionary.command.status
```

## Commands
```
# Set the answer by entity name
# Known bugs with 'set entity' command (Minecraft 1.10.2, Sponge API 5.1.0):
# Selector arguments 'c=', 'tag=', 'score_*=', 'score_*_min=' do not work. There may be more. This is an issue with Sponge, not the plugin.
# A workaround is to execute off of the entity you wish to set as the answer and make it select itself as the answer. (e.g. 'execute @r ~ ~ ~ pictionary set entity @e[r=0]')
/pictionary set entity <entity selector>

# Set the answer by string
/pictionary set string <string>

# Clear the answer manually. The answer is automatically cleared when a winner has been announced
/pictionary clear

# Status of Pictionary (whether the plugin is still waiting for an answer to be found or not). Returns success if it is not waiting for an answer
/pictionary status
```
