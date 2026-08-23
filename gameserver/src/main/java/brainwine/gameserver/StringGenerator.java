package brainwine.gameserver;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class StringGenerator {
    
    private static final String[] ZONE_PREFIXES = {
        "East", "Fort", "Lower", "Mount", "New", "North", "Old", "South", "St.", "Upper", "West", "Central", "Far", "Inner", "Outer", "Northeast", "Northwest", 
        "Southeast", "Southwest", "Northern", "Western", "Southern", "Eastern", "Downward", "Lost", "Great", "Fallen", "Crested", "Southside", "Creekside",
        "Landlocked", "Outlying", "Shallow", "Sunken", "Greater", "Overgrown", "Desolate", "Downwind", "Little", "Dry", "Forgotten", "Radiant", "Mid",
        "Wild", "Windward", "Rocky", "Inland", "Wallside", "Wayward", "Windforth"
    };
    
    public static final String[] ZONE_SUFFIXES = {
        "Bluff", "Bridge", "Canyon", "Court", "Cove", "Crossing", "Dale", "Dell", "Falls", "Field", 
        "Glade", "Glen", "Gorge", "Grove", "Heights", "Hills", "Hollow", "Keep", "Mill", "Passage", 
        "Peak", "Place", "Point", "Ridge", "Shore", "Springs", "Vale", "Valley", "Village", "Way", 
        "Woods", "Cape", "Cliffs", "Crag", "Isle", "Bog",  "Crest", "Gap", "Head", "Moor", "Reach",
        "Tundra", "Spire", "Roost", "Thicket", "Borough", "Burrow", "Overlook", "View", "Trace",
        "Refuge", "Vista", "Watch", "Yard", "Rockery", "Turnpike", "Gnoll", "Heaps", "Highland",
        "Lowland", "Midland", "Border", "Basin", "Chasm", "Gully", "Ward", "Wharf", "Pass", "Crater",
        "Ravine", "Prarie", "Upalands", "Wastes", "Wilds", "Crossing", "Gate", "Hold", "Expanse",
        "Nook", "Province", "Lands", "Lair", "Verge", "Bramble", "Steps", "Caverns", "Blind", "Summit",
        "Frontier", "Threshold", "Crossroads"
    };
    
    private static final String[] ZONE_NAMES = {
        "Abroath", "Acton", "Ashington", "Awktill", "Ayrshire", "Bakewell", "Ballysud", "Bextrast", "Bifflesud", "Biggledrip", 
        "Bigsbyshire", "Bigsbythrow", "Birkenhead", "Bixnay", "Blackcastle", "Blantyre", "Brambleclum", "Bredlyclum", "Bredlystaple", "Bunnyworth", 
        "Burford", "Butterminster", "Butterward", "Buxenbridge", "Caithness", "Callington", "Canterhersh", "Canterswin", "Casterbib", "Chatterchurch", 
        "Chesterwint", "Chickerell", "Chippinggumby", "Chippingtad", "Chumshire", "Cindermead", "Coleford", "Combbridge", "Crackwardine", "Cratham", 
        "Crickmouster", "Crouchfold", "Croydon", "Crumbibby", "Cuddlekenne", "Cuddleton", "Dallyswaine", "Danderkale", "Dandylang", "Dantyfret", 
        "Dantyswade", "Darlington", "Desborough", "Didlythwaite", "Dingstrath", "Dodgefell", "Dorchester", "Dorfenhersh", "Dorfentory", "Drogheda", 
        "Dropsud", "Dundalk", "Dungarvan", "Edenbridge", "Fidgetbridge", "Flitwick", "Funderbridge", "Ginglingwent", "Gitbridge", "Godelming", 
        "Gravesend", "Hailsham", "Heathfield", "Heathshire", "Hertford", "Holmcombe", "Holmfit", "Holmtoft", "Hufflegander", "Hunlygin", 
        "Kilbigsby", "Killarney", "Knickermere", "Larkhall", "Larkshed", "Launceston", "Lechlade", "Leftkenne", "Littleblighter", "London", 
        "Martport", "Marvotippy", "Middleham", "Mildenhall", "Mosshersh", "Mumblecoddle", "Mumblehersh", "Mumslybost", "Natherthwaite", "Neekenne", 
        "Nerdlydin", "Newbury", "Nibcastle", "Orkney", "Padendigby", "Pegglekeld", "Petabinge", "Pettipane", "Pickering", "Pillway", 
        "Pillwint", "Potton", "Princeglum", "Puttermouth", "Putterwint", "Pyllchurch", "Richmond", "Rosesidle", "Rosewouth", "Rotherham", 
        "Russetcumby", "Saltbost", "Saltbourne", "Scrimpstring", "Scrimwan", "Scrumpbourne", "Sedbergh", "Shimshot", "Shingletip", "Skipton", 
        "Slughersh", "Slugwoddly", "Snortsdin", "Southam", "Southsea", "Southwick", "Southwold", "Specklewint", "Spilsby", "Stockport", 
        "Stockton", "Stompcoddle", "Stumpchurch", "Stumpclum", "Stumpswade", "Tanlyward", "Thumbsham", "Wadfret", "Wagfield", "Watton", 
        "Wetherby", "Willowtap", "Winchelsea", "Wittlewoddly", "Wixleybost", "Wixleywicket", "Wolsingham", "Woodbridge", "Xandercott", "Yarmouth", 
        "Yateley", "Yetterbury", "Yorkhang", "Yostercumby", "Haggleford", "Keyrock", "Stainstill", "Upmoor", "Twinerow", "Langsool", "Seethrod", 
        "Picklebard", "Filgry", "Chansery", "Nimblerock", "Strapplingpoke", "Eadinglock", "Rivetton", "Speckletoad", "Dripfork", "Craneheap", "Underthorn",
        "Pecklithe", "Jamgreath", "Haysnare", "Pickridge", "Pitchnile", "Breaklathe", "Backthorpe", "Linewood", "Hingleton", "Carthtype", "Reathdrop",
        "Easelrope", "Yearncrow", "Hipplock", "Sitchery", "Blitheturn", "Himley", "Keyturn", "Loopstock", "Blackthump", "Fanthere", "Hipstood", "Yinstream",
        "Tinselhump", "Tinerprop", "Liftgnoll", "Cartheridge", "Derryburn", "Cobseal", "Bigsbury", "Flashthorne", "Trenturn", "Regalhop", "Overday",
        "Stickthyme", "Himberry", "Sherwood", "Refflebluff", "Vesslecroach", "Brokereach", "Tradspurn", "Thymmeroach", "Therrydale", "Albion", "Vilsidge",
        "Ornburn", "Carpcatch", "Slickhook", "Spincreek", "Pondleak", "Noggswood", "Hogfyre", "Leftscrim", "Denitonne", "Lickwell", "Gripburn", "Clingram"
    };
    
    public static String getRandomZoneName() {
        Random random = ThreadLocalRandom.current();
        int format = random.nextInt(3);
        
        switch(format) {
            case 0: return ZONE_NAMES[random.nextInt(ZONE_NAMES.length)];
            case 1: return getRandomName(ZONE_PREFIXES, ZONE_NAMES);
            case 2: return getRandomName(ZONE_NAMES, ZONE_SUFFIXES);
        }
        
        return "Mystery Zone"; // Should not happen
    }
    
    public static String getRandomZoneName(Function<String, Boolean> dupeCheck, int maxRetries) {
        String name = getRandomZoneName();
        int retries = 0;
        
        if(dupeCheck == null) {
            return name;
        }
        
        while(dupeCheck.apply(name)) {
            if(retries >= maxRetries) {
                return null;
            }
            
            name = getRandomZoneName();
            retries++;
        }
        
        return name;
    }
    
    private static String getRandomName(String[] first, String[] second) {
        return String.format("%s %s", first[(int)(Math.random() * first.length)], second[(int)(Math.random() * second.length)]);
    }
}
