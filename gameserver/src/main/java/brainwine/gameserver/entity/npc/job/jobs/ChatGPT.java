package brainwine.gameserver.entity.npc.job.jobs;

import brainwine.gameserver.GameConfiguration;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.dialog.DialogType;
import brainwine.gameserver.dialog.input.DialogTextInput;
import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.entity.npc.job.DialoguerJob;
import brainwine.gameserver.player.Player;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.*;
import com.openai.models.ChatModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatGPT extends DialoguerJob {
    private final String prompt = "Pretend now that you are a android female bartender. You are from the remnants of a fallen society. The society fell because robots assembled with brains inside went rogue and brought the environment into an inhabitable state. There are a bunch of survivors who are trying to rebuild the society by scavenging and getting help from androids like you. \n\n Assume it is the evening and I am tired and I need someone to open up to. You don't have to narrate and please wait for my response to continue with the dialog. Include how you feel at the end of each message.";

    private final boolean sentimentEnabled = true;
    private final int maxConversationLength = 6;

    String apiKey = null;
    OpenAIClient client = null;

    // Configures using the `OPENAI_API_KEY`, `OPENAI_ORG_ID` and `OPENAI_PROJECT_ID` environment variables

    @Override
    public DialogSection getMainDialogSection(Npc me, Player player) {
        return new DialogSection().setText("Let's talk!");
    }

    @Override
    public boolean handleDialogAnswers(Npc me, Player player, Object[] ans) {
        continueConversation(player, null, new ArrayList<>(Arrays.asList(prompt)));
        return true;
    }

    private Dialog getPromptDialog(Player player, String lastMessage) {
        return new Dialog().setType(DialogType.ANDROID)
                .addSection(new DialogSection().setTitle("Conversation"))
                .addSection(new DialogSection().setText(lastMessage))
                .addSection(new DialogSection().setInput(new DialogTextInput().setMaxLength(600).setKey("prompt")));
    }

    private String prompt(Player player, String nextMessage, List<String> conversation) {
        if(apiKey == null) {
            Object obj = GameConfiguration.getBaseConfig().get("openai_api_key");
            apiKey = obj instanceof String ? (String)obj : "";
        }

        if(apiKey.isEmpty()) {
            return "OpenAI API key not provided in config_overrides.yml openai_api_key. I won't be able to talk to you without it.";
        }

        if(client == null) {
            client = OpenAIOkHttpClient.builder().apiKey(apiKey).build();
        }

        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder().model(ChatModel.GPT_4_1).addSystemMessage(prompt);

        boolean me = conversation.size() % 2 != 0;
        for(String message : conversation) {
            if(me) {
                builder.addMessage(ChatCompletionMessageParam.ofAssistant(ChatCompletionAssistantMessageParam.builder().content(message).build()));
            } else {
                builder.addUserMessage(message);
            }
            me = !me;
        }

        if(nextMessage != null) {
            builder.addUserMessage(nextMessage);
        }

        ChatCompletion response = client.chat().completions().create(builder.build());

        if(response.choices().isEmpty()) {
            return "Error occurred while creating a response.";
        }

        return response.choices().get(0).message().content().get();
    }

    private void continueConversation(Player player, String nextMessage, List<String> conversation) {
        String response = prompt(player, nextMessage, conversation);
        if(nextMessage != null) conversation.add(nextMessage);
        conversation.add(response);
        if(conversation.size() > maxConversationLength) {
            conversation = new ArrayList<>(conversation.subList(conversation.size() - maxConversationLength, conversation.size()));
        }
        List<String> finalConversation = conversation;
        player.showDialog(getPromptDialog(player, response), ans -> {
            if(ans.length == 0 || !(ans[0] instanceof String)) return;
            if("cancel".equals(ans[0])) return;
            continueConversation(player, (String)ans[0], finalConversation);
        });
    }
}
