package brainwine.gameserver.quest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import brainwine.gameserver.GameConfiguration;
import brainwine.gameserver.util.MapHelper;

@JsonIgnoreProperties("zones")
public class Quest {
    private String id;

    @JsonProperty("group")
    private String group;

    @JsonProperty("title")
    private String title;

    @JsonProperty("reward")
    private QuestReward reward;

    @JsonProperty("story")
    private QuestStory story;

    @JsonProperty("desc")
    private String description;

    @JsonProperty(value = "desc_mobile", required = false)
    private String descriptionMobile = null;

    @JsonProperty(value = "actions", required = false)
    private Map<QuestAction.Type, List<QuestAction>> actions = new HashMap<>();

    @JsonProperty("tasks")
    private List<QuestTask> tasks;

    public static Quest get(String questId) {
        Quest quest = MapHelper.get(GameConfiguration.getBaseConfig(), questId, Quest.class);

        if (quest == null) return null;

        quest.setId(questId);
        return quest;
    }
        
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroup() {
        return group;
    }

    public String getTitle() {
        return title;
    }

    public QuestReward getReward() {
        return reward;
    }

    public QuestStory getStory() {
        return story;
    }

    public String getDescription() {
        return description;
    }

    public String getDescriptionMobile() {
        return descriptionMobile;
    }

    public Map<QuestAction.Type, List<QuestAction>> getActions() {
        return actions;
    }

    public List<QuestTask> getTasks() {
        return tasks;
    }
    
}
