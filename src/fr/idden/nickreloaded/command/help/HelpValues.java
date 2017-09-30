package fr.idden.nickreloaded.command.help;

import fr.idden.nickreloaded.api.config.Config;

public enum HelpValues
{
    MAIN_COMMAND("nickreloaded <reload, check>", Config.DESCRIPTION_MAIN.getConfigValue()),
    ADMNICK_COMMAND("admnick <unnick> <all, asideme>", Config.DESCRIPTION_ADMNICK.getConfigValue()),
    NICK_COMMAND("manager <nickname> <skin>", Config.DESCRIPTION_NICK.getConfigValue()),
    UNNICK_COMMAND("unnick", Config.DESCRIPTION_UNNICK.getConfigValue());


    String command, description;

    HelpValues(String command, String description) { this.command = command; this.description = description; }

    public String getCommand()
    {
        return command;
    }

    public String getDescription()
    {
        return description;
    }
}
