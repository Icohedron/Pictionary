# Pictionary
Pictionary plugin for the EGO Minecraft Event Server.
Provides mechanics that make Pictionary possible.

## Permissions
```
pictionary.command.answer.setbystring
pictionary.command.answer.setbyentity
pictionary.command.answer.status
pictionary.command.answer.clear

pictionary.command.artist.set
pictionary.command.artist.status
pictionary.command.artist.clear

pictionary.command.clear
```

## Commands
```
# Set the answer by entity name
/pictionary answer setbyentity <entity>
/pc answer setbyentity <entity>

# Set the answer by string
/pictionary answer setbystring <string>
/pc answer setbystring <string>

# Get the status of the answer. Returns a command success if the answer is ready to be set (i.e. there is no answer set, and it's waiting for one to be set)
/pictionary answer status
/pc answer status

# Clear the current answer
/pictionary answer clear
/pc answer clear


# Set the artist
/pictionary artist set <player>
/pc artist set <player>

# Get the status of the artist. Returns a command success if the artist is ready to be set (i.e. there is currently no artist, and it's waiting for one to be set)
/pictionary artist status
/pc artist status

# Clear the current artist
/pictioonary artist clear
/pc artist clear


# Clears both the current answer and the artist
/pictionary clear
```
