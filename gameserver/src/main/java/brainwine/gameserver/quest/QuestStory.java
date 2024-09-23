package brainwine.gameserver.quest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QuestStory {
    private String intro;
    private String accept;
    private String begin;
    @JsonProperty("begin_mobile")
    private String beginMobile;
    private String incomplete;
    private String complete;

    public String getIntro() {
        return intro;
    }
    public String getAccept() {
        return accept;
    }
    public String getBegin() {
        return begin;
    }
    public String getBeginMobile() {
        return beginMobile;
    }
    public String getIncomplete() {
        return incomplete;
    }
    public String getComplete() {
        return complete;
    }
}
