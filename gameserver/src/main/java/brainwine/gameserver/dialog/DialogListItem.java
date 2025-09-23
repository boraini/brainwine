package brainwine.gameserver.dialog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DialogListItem {
    
    private String text;
    private String image;
    private int item;
    private boolean supportRichText;
    
    public DialogListItem setText(String text) {
        this.text = text;
        return this;
    }
    
    public String getText() {
        return text;
    }
    
    public DialogListItem setImage(String image) {
        this.image = image;
        return this;
    }
    
    public String getImage() {
        return image;
    }
    
    public DialogListItem setItem(int item) {
        this.item = item;
        return this;
    }
    
    public int getItem() {
        return item;
    }

    @JsonProperty(value="supportRichText")
    public boolean isSupportRichText() {
        return supportRichText;
    }

    public DialogListItem setSupportRichText(boolean supportsRichText) {
        this.supportRichText = supportsRichText;
        return this;
    }
}
