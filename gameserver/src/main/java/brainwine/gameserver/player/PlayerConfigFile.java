package brainwine.gameserver.player;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import brainwine.gameserver.androidshop.AndroidShopHistory;
import brainwine.gameserver.mail.MailBox;
import brainwine.gameserver.quest.Quest;
import brainwine.gameserver.util.ValueWithExpiry;

import brainwine.gameserver.achievement.Achievement;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.quest.QuestProgress;
import brainwine.gameserver.zone.Zone;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerConfigFile {
    
    private String name;
    private String email;
    private String passwordHash;
    private String apiToken;
    private Zone currentZone;
    private boolean admin;
    private int experience;
    private int skillPoints;
    private int karma;
    private int crowns;
    private String displayedOrder = null;
    private Inventory inventory = new Inventory();
    private PlayerStatistics statistics = new PlayerStatistics();
    private List<String> authTokens = new ArrayList<>();
    private List<NameChange> nameChanges = new ArrayList<>();
    private List<PlayerRestriction> mutes = new ArrayList<>();
    private List<PlayerRestriction> bans = new ArrayList<>();
    private List<String> recentZones = new ArrayList<>();
    private List<String> bookmarkedZones = new ArrayList<>();
    private Set<String> followees = new HashSet<>();
    private Set<String> followers = new HashSet<>();
    private String guildId;
    private Set<String> lootCodes = new HashSet<>();
    private Set<Achievement> achievements = new HashSet<>();
    private Map<String, Integer> orders = new HashMap<>();
    private Map<String, Float> ignoredHints = new HashMap<>();
    private Map<Skill, Integer> skills = new HashMap<>();
    private Map<Item, List<Skill>> bumpedSkills = new HashMap<>();
    private Map<String, Object> appearance = new HashMap<>();
    private Map<String, QuestProgress> questProgresses = new HashMap<>();
    private ValueWithExpiry<List<Quest>> dailyQuest = ValueWithExpiry.getExpired();
    private Map<String, Quest> androidQuests = new HashMap<>();
    private AndroidShopHistory androidShopBuyHistory = new AndroidShopHistory();
    private AndroidShopHistory androidShopSellHistory = new AndroidShopHistory();
    private String familyName = null;
    private Map<String, OffsetDateTime> actionHistory = new HashMap<>();
    private MailBox mailBox = new MailBox();
    
    public PlayerConfigFile(Player player) {
        this.name = player.getName();
        this.email = player.getEmail();
        this.passwordHash = player.getPassword();
        this.apiToken = player.getApiToken();
        this.currentZone = player.getZone();
        this.admin = player.isAdmin();
        this.experience = player.getExperience();
        this.skillPoints = player.getSkillPoints();
        this.karma = player.getKarma();
        this.crowns = player.getCrowns();
        this.inventory = player.getInventory();
        this.statistics = player.getStatistics();
        this.authTokens = player.getAuthTokens();
        this.nameChanges = player.getNameChanges();
        this.mutes = player.getMutes();
        this.bans = player.getBans();
        this.recentZones = player.getRecentZones();
        this.bookmarkedZones = player.getBookmarkedZones();
        this.followees = player.getFollowees();
        this.followers = player.getFollowers();
        this.guildId = player.getGuildId();
        this.lootCodes = player.getLootCodes();
        this.achievements = player.getAchievements();
        this.orders = player.getOrders();
        this.displayedOrder = player.getDisplayedOrder();
        this.ignoredHints = player.getIgnoredHints();
        this.skills = player.getSkills();
        this.bumpedSkills = player.getBumpedSkills();
        this.appearance = player.getAppearance();
        this.questProgresses = player.getQuestProgresses();
        this.dailyQuest = player.getDailyQuest();
        this.androidQuests = player.getAndroidQuests();
        this.androidShopBuyHistory = player.getAndroidShopBuyHistory();
        this.androidShopSellHistory = player.getAndroidShopSellHistory();
        this.familyName = player.getFamilyName();
        this.actionHistory = player.getActionHistory();
        this.mailBox = player.getMailBox();
    }

    private static int transferSkill(Map<String, Integer> skills, String from, String to, int max) {
        int currentSrc = skills.getOrDefault(from, 1);
        int points = currentSrc - 1;
        int currentDest;
        if(to != null) {
            currentDest = skills.getOrDefault(to, 1);
        } else {
            currentDest = 0;
        }
        currentDest += points;
        int freePoints = 0;
        if(currentDest > max) {
            freePoints += currentDest - max;
            currentDest = max;
        }
        skills.remove(from);
        if(to != null) {
            skills.put(to, currentDest);
        }
        return freePoints;
    }

    private static void transferBumpedSkill(Map<Item, Collection<String>> bumpedSkills, String from, String to) {
        for(Collection<String> set : bumpedSkills.values()) {
            if (set.contains(from)) {
                set.remove(from);
                if(to != null) set.add(to);
            }
        }
    }

    @JsonCreator
    private PlayerConfigFile(
            @JsonSetter("skills") Map<String, Integer> skillsMap,
            @JsonSetter("bumped_skills") Map<Item, Collection<String>> bumpedSkillsMap,
            @JsonSetter("skill_points") Integer currentSkillPointsObj
    ) {
        // Transfer some skills
        int currentSkillPoints = currentSkillPointsObj != null ? currentSkillPointsObj : 0;

        currentSkillPoints += transferSkill(skillsMap, "science", "barter", Player.MAX_NATURAL_SKILL_LEVEL);
        transferBumpedSkill(bumpedSkillsMap, "science", "barter");

        currentSkillPoints += transferSkill(skillsMap, "automata", null, 0);
        transferBumpedSkill(bumpedSkillsMap, "automata", null);

        this.skillPoints = currentSkillPoints;
        for(Map.Entry<String, Integer> entry : skillsMap.entrySet()) {
            this.skills.put(Skill.fromId(entry.getKey()), entry.getValue());
        }
        for(Map.Entry<Item, Collection<String>> entry : bumpedSkillsMap.entrySet()) {
            this.bumpedSkills.put(entry.getKey(), entry.getValue().stream().map(Skill::fromId).collect(Collectors.toList()));
        }
    }
    
    @JsonSetter(nulls = Nulls.FAIL)
    public String getName() {
        return name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public String getApiToken() {
        return apiToken;
    }
    
    public Zone getCurrentZone() {
        return currentZone;
    }
    
    public boolean isAdmin() {
        return admin;
    }
    
    @JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)
    public List<String> getAuthTokens() {
        return authTokens;
    }
    
    @JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)
    public List<NameChange> getNameChanges() {
        return nameChanges;
    }
    
    @JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)
    public List<PlayerRestriction> getMutes() {
        return mutes;
    }
    
    @JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)
    public List<PlayerRestriction> getBans() {
        return bans;
    }
    
    @JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)
    public List<String> getRecentZones() {
        return recentZones;
    }
    
    @JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)
    public List<String> getBookmarkedZones() {
        return bookmarkedZones;
    }
    
    public int getExperience() {
        return experience;
    } 
    
    public int getSkillPoints() {
        return skillPoints;
    }
    
    public int getKarma() {
        return karma;
    }
    
    public int getCrowns() {
        return crowns;
    }

    public String getDisplayedOrder() {
        return displayedOrder;
    }

    @JsonSetter(nulls = Nulls.SKIP)
    public Inventory getInventory() {
        return inventory;
    }
    
    @JsonSetter(nulls = Nulls.SKIP)
    public PlayerStatistics getStatistics() {
        return statistics;
    }
    
    @JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)
    public Set<String> getFollowees() {
        return followees;
    }

    public String getGuildId() {
        return guildId;
    }
    
    @JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)
    public Set<String> getFollowers() {
        return followers;
    }
    
    @JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)
    public Set<String> getLootCodes() {
        return lootCodes;
    }
    
    @JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)
    public Set<Achievement> getAchievements() {
        return achievements;
    }

    @JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)
    public Map<String, Integer> getOrders() {
        return orders;
    }

    @JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)
    public Map<String, Float> getIgnoredHints() {
        return ignoredHints;
    }
    
    @JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)
    public Map<Skill, Integer> getSkills() {
        return skills;
    }
    
    @JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)
    public Map<Item, List<Skill>> getBumpedSkills() {
        return bumpedSkills;
    }
    
    @JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)
    public Map<String, Object> getAppearance() {
        return appearance;
    }

    @JsonSetter(nulls = Nulls.SKIP, contentNulls = Nulls.SKIP)
    public Map<String, QuestProgress> getQuestProgresses() {
        return questProgresses;
    }

    public ValueWithExpiry<List<Quest>> getDailyQuest() {
        return dailyQuest;
    }

    public Map<String, Quest> getAndroidQuests() {
        return androidQuests;
    }

    @JsonAlias("android_shop_history")
    public AndroidShopHistory getAndroidShopBuyHistory() {
        return androidShopBuyHistory;
    }

    public AndroidShopHistory getAndroidShopSellHistory() {
        return androidShopSellHistory;
    }

    public String getFamilyName() {
        return familyName;
    }

    public Map<String, OffsetDateTime> getActionHistory() {
        return actionHistory;
    }

    public MailBox getMailBox() {
        return mailBox;
    }
}
