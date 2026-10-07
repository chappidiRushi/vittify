package com.reddy.vittify.domain.catalogue

import com.reddy.vittify.data.database.entity.CategoryEntity
import com.reddy.vittify.data.database.entity.SubcategoryEntity
import java.util.Locale

/**
 * Represents a matched suggestion chip for the category picker.
 */
data class QuickSuggestionMatch(
    val category: CategoryEntity,
    val subcategory: SubcategoryEntity?,
    val matchedItem: String? = null,
    val score: Int = 0
)

/**
 * Catalogue entry mapping category & subcategory to everyday items, goods, services, and keywords.
 */
data class SubcategoryItemEntry(
    val category: String,
    val subcategory: String,
    val items: List<String>
)

/**
 * Centralized, exhaustive catalogue of items mapped to subcategories and categories.
 * Powers smart semantic search in category chooser search boxes so typing items
 * like "milk" suggests "Dairy", "carrot" suggests "Vegetables", "petrol" suggests "Fuel", etc.
 */
object CategoryItemCatalogue {

    private val ENTRIES: List<SubcategoryItemEntry> = listOf(
        // ==========================================
        // 1. FOOD & DRINKS
        // ==========================================
        SubcategoryItemEntry(
            category = "Food & Drinks",
            subcategory = "Eating out",
            items = listOf(
                "dine in", "dining", "dinner", "lunch", "breakfast", "brunch", "restaurant",
                "cafe", "buffet", "bistro", "dhaba", "fine dining", "thali", "mess", "food court",
                "eatery", "steakhouse", "sushi bar", "barbecue", "bbq", "sizzler", "hotel food",
                "trattoria", "diner", "brasserie", "izakaya", "rooftop restaurant", "udipi",
                "sagar", "saravana bhavan", "biryani", "dosa", "thali meal", "meals", "veg meals",
                "non veg meals", "chinese restaurant", "italian restaurant", "mexican food",
                "lebanese", "continental", "south indian", "north indian", "tandoori", "kabab",
                "curry", "rice plate", "canteen", "cafeteria"
            )
        ),
        SubcategoryItemEntry(
            category = "Food & Drinks",
            subcategory = "Take Away",
            items = listOf(
                "takeout", "take away", "takeaway", "parcel", "parcel food", "pack food",
                "pick up food", "drive thru", "drive through", "food parcel", "carry out",
                "to go", "takeaway order", "packed lunch", "food box", "packed dinner",
                "curbside pickup"
            )
        ),
        SubcategoryItemEntry(
            category = "Food & Drinks",
            subcategory = "Tea & Coffee",
            items = listOf(
                "tea", "coffee", "chai", "tapri", "cutting chai", "masala chai", "ginger tea",
                "adrak chai", "green tea", "black tea", "lemon tea", "iced tea", "herbal tea",
                "chamomile tea", "earl grey", "matcha", "oolong", "latte", "cappuccino",
                "espresso", "mocha", "americano", "cold brew", "iced coffee", "filter coffee",
                "macchiato", "flat white", "affogato", "cortado", "frappuccino", "frappe",
                "starbucks", "barista", "blue tokai", "costa coffee", "cafe coffee day", "ccd",
                "third wave", "sleepy owl", "roastery", "chaayos", "chai point", "bubble tea",
                "boba", "boba tea", "kombucha", "chicory", "dunkin", "tea bag", "coffee beans",
                "instant coffee", "nescafe", "bru"
            )
        ),
        SubcategoryItemEntry(
            category = "Food & Drinks",
            subcategory = "Fast Food",
            items = listOf(
                "fast food", "burger", "hamburger", "cheeseburger", "veggie burger", "chicken burger",
                "fries", "french fries", "potato wedges", "nuggets", "chicken nuggets", "hot dog",
                "tacos", "nachos", "burritos", "quesadilla", "churros", "fried chicken", "kfc",
                "mcdonalds", "mc donalds", "subway", "burger king", "wendy's", "taco bell",
                "popeyes", "roll", "wraps", "kathi roll", "shawarma", "frankie", "momos",
                "steamed momos", "fried momos", "spring roll", "samosa", "kachori", "chaat",
                "panipuri", "pani puri", "golgappa", "puchka", "bhelpuri", "bhel puri",
                "sev puri", "dahi puri", "pav bhaji", "vada pav", "batata vada", "misal pav",
                "onion rings", "hot wings", "crispy chicken", "slider", "falafel"
            )
        ),
        SubcategoryItemEntry(
            category = "Food & Drinks",
            subcategory = "Snacks",
            items = listOf(
                "snack", "snacks", "chips", "potato chips", "crisps", "lays", "kurkure", "doritos",
                "pringles", "popcorn", "caramel popcorn", "salted popcorn", "peanuts",
                "roasted peanuts", "roasted nuts", "almonds", "cashews", "kaju", "badam",
                "pistachios", "namkeen", "bhujia", "aloo bhujia", "sev", "ratlami sev",
                "mixture", "chivda", "farsan", "wafers", "banana chips", "crackers", "pretzels",
                "biscuits", "parle g", "marie gold", "bourbon", "oreo", "hide and seek",
                "good day", "rusk", "cookies", "butter cookies", "choco chip cookies", "munchies",
                "trail mix", "bar snack", "puff", "veg puff", "egg puff", "chicken puff",
                "samosa snack", "mathri", "murukku", "chakli", "khakhra", "bhakarwadi"
            )
        ),
        SubcategoryItemEntry(
            category = "Food & Drinks",
            subcategory = "Swiggy",
            items = listOf(
                "swiggy", "swiggy dineout", "swiggy delivery", "swiggy one", "swiggy genie",
                "swiggy gourmet", "swiggy food", "swiggy order"
            )
        ),
        SubcategoryItemEntry(
            category = "Food & Drinks",
            subcategory = "Zomato",
            items = listOf(
                "zomato", "zomato gold", "zomato delivery", "zomato pro", "zomato food",
                "zomato order", "blinkit food"
            )
        ),
        SubcategoryItemEntry(
            category = "Food & Drinks",
            subcategory = "Sweets",
            items = listOf(
                "sweets", "sweet", "mithai", "dessert", "desserts", "gulab jamun", "rasgulla",
                "jalebi", "barfi", "kaju katli", "halwa", "gajar halwa", "laddu", "ladoo",
                "motichoor laddu", "besan laddu", "peda", "rasmalai", "rabdi", "kheer",
                "payasam", "shrikhand", "mysore pak", "soan papdi", "ice cream", "vanilla ice cream",
                "chocolate ice cream", "gelato", "popsicle", "sundae", "kulfi", "matka kulfi",
                "cornetto", "magnum", "chocobar", "chocolate", "chocolates", "dark chocolate",
                "dairy milk", "cadbury", "ferrero rocher", "kitkat", "snickers", "waffle",
                "waffles", "pancake", "pancakes", "crepe", "brownies", "walnut brownie",
                "sizzling brownie", "pudding", "custard", "mousse", "pastry", "pineapple pastry",
                "black forest", "red velvet", "cheesecake", "tiramisu", "tart", "baklava",
                "modak", "cham cham", "kalakand", "basundi"
            )
        ),
        SubcategoryItemEntry(
            category = "Food & Drinks",
            subcategory = "Liquor",
            items = listOf(
                "liquor", "alcohol", "booze", "beer", "craft beer", "draught beer", "kingfisher",
                "budweiser", "heineken", "bira", "corona", "tuborg", "carlsberg", "wine",
                "red wine", "white wine", "rose wine", "sparkling wine", "sula", "jacob's creek",
                "whiskey", "whisky", "scotch", "single malt", "blended scotch", "bourbon",
                "johnnie walker", "glenlivet", "chivas", "jack daniels", "jameson", "old monk",
                "teachers", "100 pipers", "vodka", "absolut", "smirnoff", "grey goose",
                "magic moments", "rum", "bacardi", "captain morgan", "gin", "bombay sapphire",
                "tanqueray", "hendricks", "greater than", "tequila", "jose cuervo", "cocktail",
                "cocktails", "cosmopolitan", "margarita", "mojito", "long island", "sangria",
                "martini", "mocktail", "brandy", "champagne", "pub", "bar", "brewery",
                "microbrewery", "bottle shop", "tasmac", "wine shop", "liquor store", "beer shop",
                "theka", "drinks party", "tavern", "tonic"
            )
        ),
        SubcategoryItemEntry(
            category = "Food & Drinks",
            subcategory = "Beverages",
            items = listOf(
                "beverages", "beverage", "soda", "soft drink", "carbonated drink", "coke",
                "coca cola", "pepsi", "sprite", "thums up", "fanta", "mirinda", "7up",
                "mountain dew", "limca", "ginger ale", "tonic water", "juice", "fresh juice",
                "fruit juice", "orange juice", "apple juice", "mango juice", "sugarcane juice",
                "mosambi juice", "real juice", "tropicana", "smoothie", "berry smoothie",
                "banana smoothie", "milkshake", "chocolate milkshake", "strawberry shake",
                "thickshake", "coconut water", "nariyal pani", "energy drink", "red bull",
                "monster", "sting", "ocean water", "lemon soda", "nimbu pani", "lemonade",
                "lassi", "sweet lassi", "mango lassi", "buttermilk", "chaas", "gatorade",
                "glucon d", "squash", "rooh afza", "sparkling water", "packaged water"
            )
        ),
        SubcategoryItemEntry(
            category = "Food & Drinks",
            subcategory = "Date",
            items = listOf(
                "date", "date night", "romantic dinner", "candle light dinner", "date dining",
                "couple dinner", "anniversary dinner", "fine dining date", "rooftop date"
            )
        ),
        SubcategoryItemEntry(
            category = "Food & Drinks",
            subcategory = "Pizza",
            items = listOf(
                "pizza", "pizzas", "pizza slice", "margherita", "pepperoni pizza",
                "farmhouse pizza", "cheese burst", "garlic bread", "stuffed garlic bread",
                "calzone", "dominos", "domino's", "pizza hut", "mojo pizza", "la pinoz",
                "ovenstory", "papa johns", "thin crust", "wood fired pizza", "pan pizza",
                "deep dish", "pasta pizza", "pizza crust"
            )
        ),
        SubcategoryItemEntry(
            category = "Food & Drinks",
            subcategory = "Tiffin",
            items = listOf(
                "tiffin", "dabbawala", "dabba", "home food", "tiffin service", "mess food",
                "daily meal box", "bento box", "idli", "idli sambar", "dosa", "masala dosa",
                "plain dosa", "mysore masala dosa", "rava dosa", "ghee roast", "vada",
                "medu vada", "uttapam", "onion uttapam", "pongal", "poori", "puri",
                "poori bhaji", "chole bhature", "paratha", "aloo paratha", "paneer paratha",
                "gobi paratha", "thepla", "appam", "puttu", "upma", "sheera", "poha", "kanda poha"
            )
        ),

        // ==========================================
        // 2. TRANSPORT
        // ==========================================
        SubcategoryItemEntry(
            category = "Transport",
            subcategory = "Uber",
            items = listOf(
                "uber", "uber cab", "uber taxi", "uber auto", "uber moto", "uber trip",
                "uber ride", "uber pool", "uber premier", "uber go", "uber xl", "uber rentals",
                "uber package", "uber reserve"
            )
        ),
        SubcategoryItemEntry(
            category = "Transport",
            subcategory = "Rapido",
            items = listOf(
                "rapido", "rapido bike", "rapido auto", "rapido cab", "rapido captain",
                "rapido ride", "bike taxi rapido"
            )
        ),
        SubcategoryItemEntry(
            category = "Transport",
            subcategory = "Auto",
            items = listOf(
                "auto", "auto rickshaw", "autorickshaw", "tuktuk", "tuk tuk", "meter auto",
                "shared auto", "auto fare", "rickshaw", "e-rickshaw", "erickshaw", "tempo auto"
            )
        ),
        SubcategoryItemEntry(
            category = "Transport",
            subcategory = "Cab",
            items = listOf(
                "cab", "taxi", "ola", "ola cab", "ola mini", "ola prime", "blusmart",
                "blu smart", "mega cabs", "rental cab", "outstation cab", "radio taxi",
                "cab fare", "cab booking", "taxi meter", "prepaid taxi", "airport cab"
            )
        ),
        SubcategoryItemEntry(
            category = "Transport",
            subcategory = "Train",
            items = listOf(
                "train", "irctc", "railways", "indian railways", "train ticket", "railway ticket",
                "local train", "passenger train", "express train", "vande bharat", "shatabdi",
                "rajdhani", "duronto", "tejas", "garib rath", "sleeper coach", "ac coach",
                "train pass", "season ticket", "tatkal", "platform ticket", "irctc catering",
                "train berth", "train fare"
            )
        ),
        SubcategoryItemEntry(
            category = "Transport",
            subcategory = "Metro",
            items = listOf(
                "metro", "subway", "underground train", "metro smart card", "metro card",
                "metro token", "metro recharge", "dmrc", "nmrc", "bmrcl", "namma metro",
                "mmrcl", "mumbai metro", "hyderabad metro", "chennai metro", "kolkata metro",
                "metro pass", "rapid metro"
            )
        ),
        SubcategoryItemEntry(
            category = "Transport",
            subcategory = "Bus",
            items = listOf(
                "bus", "city bus", "bus ticket", "public transport", "volvo", "ksrtc", "msrtc",
                "dtc", "bmtc", "tsrtc", "apsrtc", "upsrtc", "hrtc", "redbus", "red bus",
                "abhibus", "chalo", "chalo bus", "bus pass", "state transport", "interstate bus",
                "sleeper bus", "ac bus", "private bus", "zingbus", "intracity bus"
            )
        ),
        SubcategoryItemEntry(
            category = "Transport",
            subcategory = "Bike",
            items = listOf(
                "bike", "motorcycle", "scooty", "scooter", "two wheeler", "bike taxi",
                "bike rental", "royal enfield", "honda activa", "tvs jupiter", "bounce",
                "vogo", "yulu", "royal brothers", "bicycle", "cycle rental", "ev bike",
                "motorbike ride"
            )
        ),
        SubcategoryItemEntry(
            category = "Transport",
            subcategory = "Fuel",
            items = listOf(
                "fuel", "petrol", "diesel", "cng", "gas station", "petrol pump", "gas pump",
                "fuel filling", "shell", "shell petrol", "indian oil", "iocl", "hpcl",
                "hp petrol", "bpcl", "bharat petroleum", "lpg auto gas", "power petrol",
                "speed petrol", "xp95", "extra premium", "octane"
            )
        ),
        SubcategoryItemEntry(
            category = "Transport",
            subcategory = "Ev Charge",
            items = listOf(
                "ev charge", "ev charging", "electric vehicle charging", "ather grid",
                "tata power ev", "zeon", "charge zone", "ev station", "electric scooter charge",
                "electric car charging", "kwh charge", "ev fast charging", "statiq"
            )
        ),
        SubcategoryItemEntry(
            category = "Transport",
            subcategory = "Flights",
            items = listOf(
                "flight", "flights", "airline", "airplane", "plane ticket", "air ticket",
                "airfare", "airport", "boarding pass", "indigo", "air india", "vistara",
                "spicejet", "akasa air", "emirates", "qatar airways", "singapore airlines",
                "flight booking", "makemytrip flight", "skyscanner", "baggage fee",
                "web check-in", "seat selection", "airport transit", "air travel"
            )
        ),
        SubcategoryItemEntry(
            category = "Transport",
            subcategory = "Parking",
            items = listOf(
                "parking", "car parking", "bike parking", "valet parking", "parking ticket",
                "parking fee", "parking meter", "airport parking", "mall parking",
                "station parking", "monthly parking", "multi level parking", "fastag parking"
            )
        ),
        SubcategoryItemEntry(
            category = "Transport",
            subcategory = "FASTag",
            items = listOf(
                "fastag", "fast tag", "toll fastag", "paytm fastag", "icici fastag",
                "nhai fastag", "fastag recharge", "idfc fastag", "sbi fastag", "kotak fastag"
            )
        ),
        SubcategoryItemEntry(
            category = "Transport",
            subcategory = "Tolls",
            items = listOf(
                "toll", "tolls", "toll gate", "toll plaza", "toll booth", "expressway toll",
                "highway toll", "sea link toll", "bridge toll", "tunnel toll", "toll tax"
            )
        ),
        SubcategoryItemEntry(
            category = "Transport",
            subcategory = "Lounge",
            items = listOf(
                "airport lounge", "lounge access", "lounge fee", "dreamfolks", "priority pass",
                "encalm lounge", "plaza premium", "080 lounge", "adani lounge", "loyalty lounge"
            )
        ),
        SubcategoryItemEntry(
            category = "Transport",
            subcategory = "Fine",
            items = listOf(
                "fine", "traffic fine", "traffic challan", "echallan", "e-challan",
                "speeding ticket", "red light violation", "helmet fine", "towing charge",
                "parking fine", "police fine", "traffic penalty", "rto fine"
            )
        ),

        // ==========================================
        // 3. SHOPPING
        // ==========================================
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Clothes",
            items = listOf(
                "clothes", "clothing", "apparel", "shirt", "formal shirt", "casual shirt",
                "t-shirt", "tee", "tshirt", "polo shirt", "jeans", "denim", "pants", "trousers",
                "chinos", "shorts", "bermudas", "dress", "maxi dress", "skirt", "top", "blouse",
                "suit", "blazer", "jacket", "leather jacket", "coat", "winter coat", "sweater",
                "cardigan", "hoodie", "sweatshirt", "saree", "sari", "silk saree", "kurta",
                "kurti", "salwar", "salwar kameez", "lehenga", "dupatta", "churidar", "sherwani",
                "lungi", "dhoti", "innerwear", "underwear", "boxers", "briefs", "lingerie",
                "bra", "panties", "thermal wear", "socks", "nightwear", "pyjamas", "pajama",
                "track pants", "gym wear", "activewear", "sportswear", "swimwear", "zara",
                "h&m", "uniqlo", "myntra", "ajio", "westside", "max fashion", "pantaloons",
                "marks and spencer", "levis"
            )
        ),
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Footwear",
            items = listOf(
                "footwear", "shoes", "sneakers", "trainers", "running shoes", "walking shoes",
                "sports shoes", "formal shoes", "oxford shoes", "derby shoes", "boots",
                "chelsea boots", "sandals", "floaters", "slippers", "chappal", "hawai chappal",
                "flip flops", "heels", "high heels", "stilettos", "wedges", "loafers", "flats",
                "ballerinas", "crocs", "clogs", "moccasins", "insoles", "shoe polish",
                "shoe laces", "nike", "adidas", "puma", "reebok", "asics", "new balance",
                "sketchers", "woodland", "bata", "metro shoes", "red tape", "mochi", "clarks"
            )
        ),
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Electronics",
            items = listOf(
                "electronics", "gadget", "gadgets", "laptop", "notebook", "macbook", "computer",
                "pc", "desktop", "tablet", "ipad", "galaxy tab", "smartphone", "mobile phone",
                "iphone", "samsung galaxy", "oneplus", "headphones", "over ear headphones",
                "noise cancelling headphones", "earphones", "airpods", "earbuds", "tws",
                "bluetooth headset", "smartwatch", "smart watch", "apple watch", "fitbit",
                "galaxy watch", "charger", "fast charger", "charging adapter", "cable",
                "lightning cable", "type c cable", "usb cable", "hdmi cable", "powerbank",
                "power bank", "monitor", "external display", "keyboard", "mechanical keyboard",
                "mouse", "wireless mouse", "trackpad", "speaker", "bluetooth speaker",
                "soundbar", "smart speaker", "alexa", "echo dot", "google home", "camera",
                "dslr", "mirrorless camera", "action camera", "gopro", "camera lens", "tripod",
                "memory card", "sd card", "pendrive", "hard disk", "external hdd", "ssd",
                "external ssd", "webcam", "microphone", "airtag"
            )
        ),
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Festival",
            items = listOf(
                "festival", "diwali", "deepavali", "holi", "christmas", "eid", "new year",
                "durga puja", "navratri", "ganesh chaturthi", "onam", "pongal", "rakhi",
                "raksha bandhan", "bhai dooj", "crackers", "firecrackers", "fireworks",
                "sparklers", "rangoli", "rangoli colors", "gulal", "pichkari", "lantern",
                "kandil", "diya", "festive lights", "christmas tree", "christmas ornaments",
                "santa cap", "festival decoration", "festival shopping"
            )
        ),
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Video games",
            items = listOf(
                "video games", "gaming", "video game", "playstation", "ps5", "ps4", "xbox",
                "xbox series x", "nintendo", "nintendo switch", "steam", "steam deck",
                "steam games", "epic games", "gog", "console", "game disc", "controller",
                "dualsense", "dualshock", "xbox controller", "joystick", "gaming mouse",
                "gaming keyboard", "gaming headset", "vr headset", "meta quest", "gpu",
                "graphics card", "rtx", "gaming pc", "in-game purchase", "roblox", "v-bucks",
                "game skins", "dlc"
            )
        ),
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Books",
            items = listOf(
                "books", "book", "novel", "fiction", "non fiction", "biography",
                "autobiography", "textbook", "academic books", "course books", "reference book",
                "comic", "comics", "graphic novel", "manga", "anime manga", "kindle",
                "kindle book", "audible", "audio book", "ebook", "paperback", "hardcover",
                "bookmark", "dictionary", "atlas", "encyclopedia", "bookstore", "crossword"
            )
        ),
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Plants",
            items = listOf(
                "plants", "plant", "indoor plant", "outdoor plant", "flowers", "bouquet",
                "nursery", "plant nursery", "seeds", "flower seeds", "vegetable seeds",
                "soil", "potting mix", "organic compost", "fertilizer", "cocopeat",
                "flower pot", "terracotta pot", "ceramic pot", "plastic pot", "planter",
                "succulent", "succulents", "cactus", "bonsai", "money plant", "snake plant",
                "monstera", "gardening", "gardening tools", "trowel", "pruner", "watering can"
            )
        ),
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Jewellery",
            items = listOf(
                "jewellery", "jewelry", "gold", "gold chain", "gold ring", "gold coin",
                "gold biscuit", "silver", "silver coin", "silver anklet", "diamond",
                "diamond ring", "diamond necklace", "ring", "engagement ring", "wedding ring",
                "necklace", "choker", "bracelet", "bangles", "kadas", "earrings", "jhumkas",
                "studs", "chain", "pendant", "nose pin", "anklet", "payal", "brooches",
                "cufflinks", "platinum", "gemstone", "ruby", "emerald", "sapphire", "tanishq",
                "malabar gold", "kalyan jewellers", "caratlane", "bluestone"
            )
        ),
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Furniture",
            items = listOf(
                "furniture", "chair", "ergonomic chair", "office chair", "study chair",
                "gaming chair", "armchair", "rocking chair", "stools", "table", "study table",
                "work desk", "standing desk", "computer table", "dining table", "coffee table",
                "center table", "bedside table", "bed", "king size bed", "queen size bed",
                "single bed", "sofa bed", "mattress", "memory foam mattress", "latex mattress",
                "pillow", "pillows", "sofa", "couch", "recliner", "wardrobe", "almirah",
                "cupboard", "bookshelf", "book rack", "tv unit", "shoe rack", "shoe cabinet",
                "dressing table", "ikea", "pepperfry", "urban ladder", "wakefit"
            )
        ),
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Appliances",
            items = listOf(
                "appliances", "home appliances", "refrigerator", "fridge", "single door fridge",
                "double door fridge", "washing machine", "front load", "top load", "microwave",
                "microwave oven", "convection oven", "air fryer", "otg", "dishwasher",
                "air conditioner", "ac", "split ac", "inverter ac", "mixer", "grinder",
                "mixie", "blender", "hand blender", "juicer", "toaster", "electric kettle",
                "induction cooktop", "induction stove", "iron", "steam iron", "garment steamer",
                "vacuum cleaner", "robot vacuum", "water purifier", "ro purifier", "geyser",
                "water heater", "room heater", "air purifier", "exhaust fan", "ceiling fan"
            )
        ),
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Utensils",
            items = listOf(
                "utensils", "kitchen utensils", "cookware", "frying pan", "tawa", "kadai",
                "wok", "sauce pan", "sauce pot", "pressure cooker", "prestige cooker",
                "hawkins", "non stick pan", "cast iron skillet", "plates", "dinner set",
                "bowls", "cups", "coffee mug", "tea cup", "glassware", "water bottle",
                "insulated flask", "thermos", "spoons", "teaspoons", "forks", "table knives",
                "chef knife", "cutlery set", "spatula", "ladle", "tongs", "chimta",
                "rolling pin", "belan", "cutting board", "chopping board", "colander",
                "strainer", "grater", "peeler", "storage containers", "airtight jars", "dabba"
            )
        ),
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Vehicle",
            items = listOf(
                "vehicle purchase", "new car", "used car", "bike purchase", "new bike",
                "scooter purchase", "car booking", "bike booking", "vehicle down payment",
                "car accessories", "dash cam", "seat covers", "floor mats", "car perfume",
                "helmet", "riding jacket", "riding gloves", "alloy wheels"
            )
        ),
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Cosmetics",
            items = listOf(
                "cosmetics", "makeup", "lipstick", "liquid lipstick", "lip balm", "lip gloss",
                "foundation", "concealer", "compact powder", "loose powder", "bb cream",
                "cc cream", "mascara", "eyeliner", "kajal", "eye shadow", "blush", "contour",
                "highlighter", "makeup brush", "beauty blender", "nail polish", "nail paint",
                "nail remover", "perfume", "eau de parfum", "body mist", "body spray",
                "deodorant", "deo", "roll on", "fragrance", "cologne", "skin care", "skincare",
                "face wash", "cleanser", "toner", "moisturizer", "face cream", "sunscreen",
                "sunblock", "serum", "vitamin c serum", "hyaluronic acid", "sheet mask",
                "face scrub", "face pack", "eye cream", "nykaa", "sephora", "tira",
                "sugar cosmetics", "mamaearth", "the ordinary"
            )
        ),
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Toys",
            items = listOf(
                "toys", "toy", "kids toy", "lego", "lego sets", "building blocks",
                "action figure", "superheroes", "doll", "barbie", "barbie doll", "soft toy",
                "stuffed animal", "teddy bear", "board game", "monopoly", "scrabble", "chess",
                "carrom", "puzzles", "jigsaw puzzle", "rc car", "remote control car",
                "hot wheels", "toy train", "nerf gun", "play doh", "slime", "bubbles",
                "educational toys", "baby rattle", "hamleys"
            )
        ),
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Stationery",
            items = listOf(
                "stationery", "pens", "ball pen", "gel pen", "fountain pen", "rollerball",
                "pencil", "mechanical pencil", "notebook", "spiral notebook", "long book",
                "register", "dairy", "diary", "planner", "bullet journal", "notepad",
                "sticky notes", "post-it", "eraser", "sharpener", "ruler", "scale", "marker",
                "whiteboard marker", "highlighter", "sketch pens", "crayons", "paint brush",
                "canvas board", "scissors", "craft paper", "tape", "cello tape", "glue",
                "fevicol", "stapler", "staples", "paper clips", "punch machine", "pencil pouch",
                "file", "folder", "document organizer"
            )
        ),
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Glasses",
            items = listOf(
                "glasses", "eyeglasses", "spectacles", "specs", "reading glasses",
                "power glasses", "frames", "spectacle frames", "sunglasses", "shades",
                "polarized sunglasses", "aviators", "wayfarers", "contact lenses",
                "lens solution", "blue light glasses", "computer glasses", "lenskart",
                "specsmakers", "ray ban", "fastrack"
            )
        ),
        SubcategoryItemEntry(
            category = "Shopping",
            subcategory = "Devotional",
            items = listOf(
                "devotional", "puja items", "pooja samagri", "agarbatti", "incense sticks",
                "dhoop", "diya", "oil lamp", "cotton wicks", "batti", "camphor", "kapoor",
                "pooja thali", "bell", "ghanti", "shankh", "idol", "god murti", "ganesh idol",
                "krishna murti", "photo frame god", "gangajal", "kumkum", "sindoor",
                "chandan", "prasad", "prayer mat", "mala", "japa mala", "rudraksha",
                "havan samagri", "bhagavad gita"
            )
        ),

        // ==========================================
        // 4. GROCERIES
        // ==========================================
        SubcategoryItemEntry(
            category = "Groceries",
            subcategory = "Staples",
            items = listOf(
                "staples", "rice", "basmati rice", "sona masoori", "brown rice", "raw rice",
                "dal", "pulses", "lentils", "toor dal", "moong dal", "chana dal", "urad dal",
                "masoor dal", "rajma", "kidney beans", "chole", "chickpeas", "kabuli chana",
                "black chana", "green moong", "flour", "atta", "wheat flour", "chakki atta",
                "maida", "sooji", "semolina", "rava", "besan", "gram flour", "ragi flour",
                "oats", "rolled oats", "cooking oil", "vegetable oil", "sunflower oil",
                "mustard oil", "groundnut oil", "olive oil", "ghee", "cow ghee", "desi ghee",
                "sugar", "white sugar", "brown sugar", "jaggery", "gur", "honey", "salt",
                "rock salt", "spices", "masala", "turmeric", "haldi", "red chilli powder",
                "coriander powder", "cumin", "jeera", "mustard seeds", "black pepper",
                "kali mirch", "clove", "cardamom", "cinnamon", "garam masala", "pasta",
                "macaroni", "noodles", "maggi", "instant noodles", "poha", "sabudana",
                "soya chunks", "baking powder", "yeast", "vinegar", "tomato ketchup"
            )
        ),
        SubcategoryItemEntry(
            category = "Groceries",
            subcategory = "Vegetables",
            items = listOf(
                "vegetables", "vegetable", "veggies", "fresh vegetables", "sabzi", "greens",
                "carrot", "carrots", "gajar", "potato", "potatoes", "aloo", "sweet potato",
                "shakarkandi", "onion", "onions", "pyaaz", "shallots", "spring onion",
                "tomato", "tomatoes", "tamatar", "cherry tomatoes", "garlic", "lasan",
                "ginger", "adrak", "green chilli", "hari mirch", "capsicum", "bell pepper",
                "shimla mirch", "spinach", "palak", "methi leaves", "coriander", "coriander leaves",
                "cilantro", "dhania patta", "mint", "mint leaves", "pudina", "curry leaves",
                "broccoli", "cauliflower", "gobi", "cabbage", "patta gobi", "green peas",
                "peas", "matar", "beans", "french beans", "ladyfinger", "okra", "bhindi",
                "brinjal", "eggplant", "baingan", "beetroot", "beet", "cucumber", "kheera",
                "zucchini", "bottle gourd", "lauki", "bitter gourd", "karela", "ridge gourd",
                "pumpkin", "kaddu", "mushroom", "mushrooms", "corn", "sweet corn", "baby corn",
                "lettuce", "radish", "mooli", "drumstick", "lemon", "lime", "nimbu"
            )
        ),
        SubcategoryItemEntry(
            category = "Groceries",
            subcategory = "Fruits",
            items = listOf(
                "fruits", "fruit", "fresh fruits", "phal", "apple", "apples", "kashmiri apple",
                "banana", "bananas", "kela", "elakki banana", "mango", "mangoes", "aam",
                "alphonso", "kesar", "orange", "oranges", "santra", "sweet lime", "mosambi",
                "grapes", "green grapes", "black grapes", "watermelon", "tarbooz", "musk melon",
                "kharbuja", "papaya", "papita", "strawberry", "strawberries", "pineapple",
                "ananas", "pomegranate", "anar", "guava", "amrood", "kiwi", "pear", "nashpati",
                "plum", "peach", "apricot", "cherry", "cherries", "berries", "blueberry",
                "blueberries", "avocado", "dragon fruit", "custard apple", "sitaphal",
                "chikoo", "fig", "anjeer", "litchi", "coconut", "tender coconut", "daak"
            )
        ),
        SubcategoryItemEntry(
            category = "Groceries",
            subcategory = "Meat",
            items = listOf(
                "meat", "fresh meat", "raw meat", "chicken", "whole chicken", "chicken breast",
                "chicken curry cut", "chicken boneless", "chicken wings", "chicken drumstick",
                "chicken keema", "mutton", "lamb", "goat meat", "mutton curry cut", "mutton chops",
                "mutton keema", "fish", "fresh fish", "pomfret", "surmai", "salmon", "tilapia",
                "basa", "tuna", "prawns", "shrimp", "tiger prawns", "seafood", "crab", "crabs",
                "lobster", "squid", "pork", "bacon", "sausages", "salami", "beef", "steak",
                "licious", "freshmeat", "meat delivery"
            )
        ),
        SubcategoryItemEntry(
            category = "Groceries",
            subcategory = "Eggs",
            items = listOf(
                "egg", "eggs", "anda", "brown eggs", "white eggs", "farm eggs", "organic eggs",
                "free range eggs", "omega 3 eggs", "egg tray", "half dozen eggs", "dozen eggs",
                "quail eggs"
            )
        ),
        SubcategoryItemEntry(
            category = "Groceries",
            subcategory = "Bakery",
            items = listOf(
                "bakery", "bread", "white bread", "brown bread", "whole wheat bread",
                "multigrain bread", "atta bread", "sourdough", "baguette", "pita bread",
                "focaccia", "buns", "burger buns", "pav", "ladi pav", "hot dog buns",
                "sliced bread", "toast", "rusk", "milk rusk", "croissant", "butter croissant",
                "bagel", "muffins", "chocolate muffin", "cupcake", "pastry", "sponge cake",
                "fruit cake", "banana bread", "tea cake"
            )
        ),
        SubcategoryItemEntry(
            category = "Groceries",
            subcategory = "Dairy",
            items = listOf(
                "dairy", "milk", "whole milk", "full cream milk", "toned milk", "double toned milk",
                "cow milk", "buffalo milk", "pouch milk", "tetra pack milk", "skimmed milk",
                "organic milk", "a2 milk", "lactose free milk", "amul milk", "mother dairy",
                "nandini", "doodh", "curds", "curd", "dahi", "set curd", "greek yogurt",
                "yogurt", "epigamia", "buttermilk", "chaas", "lassi", "paneer", "malai paneer",
                "fresh paneer", "cottage cheese", "tofu", "butter", "amul butter", "salted butter",
                "unsalted butter", "white butter", "makkhan", "table butter", "cheese",
                "cheese slices", "cheese cubes", "mozzarella", "cheddar", "parmesan",
                "pizza cheese", "shredded cheese", "cheese spread", "cream cheese", "ghee",
                "desi ghee", "pure ghee", "cream", "fresh cream", "whipping cream", "malai",
                "condensed milk", "milkmaid", "khoya", "mawa", "milk powder", "dairy whitener",
                "oat milk", "almond milk", "soy milk"
            )
        ),
        SubcategoryItemEntry(
            category = "Groceries",
            subcategory = "Zepto",
            items = listOf(
                "zepto", "zepto pass", "zepto grocery", "zepto delivery", "blinkit",
                "blinkit delivery", "instamart", "swiggy instamart", "bigbasket", "bb daily",
                "bb now", "dunzo", "dunzo daily", "country delight", "milk basket", "quick commerce"
            )
        ),

        // ==========================================
        // 5. HOME
        // ==========================================
        SubcategoryItemEntry(
            category = "Home",
            subcategory = "Essentials",
            items = listOf(
                "home essentials", "foil", "aluminium foil", "cling film", "food wrap",
                "garbage bags", "trash bags", "dustbin bags", "tissue", "tissue paper",
                "paper napkins", "paper towels", "kitchen roll", "toilet roll", "toilet paper",
                "facial tissues", "wipes", "wet wipes", "disinfectant wipes", "batteries",
                "aa batteries", "aaa batteries", "duracell", "torch", "bulb", "bulbs",
                "led bulb", "tube light", "matchbox", "candle", "extension board"
            )
        ),
        SubcategoryItemEntry(
            category = "Home",
            subcategory = "Toiletries",
            items = listOf(
                "toiletries", "bath", "shower", "soap", "bathing soap", "beauty bar", "dove",
                "pears", "dettol soap", "body wash", "shower gel", "loofah", "shampoo",
                "head and shoulders", "pantene", "tresemme", "anti dandruff shampoo",
                "hair conditioner", "hair oil", "coconut oil", "toothpaste", "colgate",
                "sensodyne", "closeup", "toothbrush", "electric toothbrush", "mouthwash",
                "listerine", "dental floss", "hand wash", "handwash", "liquid hand soap",
                "shaving cream", "shaving gel", "razor", "shaving blade", "gillette",
                "body lotion", "vaseline", "nivea lotion", "talcum powder", "cotton buds"
            )
        ),
        SubcategoryItemEntry(
            category = "Home",
            subcategory = "Decor",
            items = listOf(
                "decor", "home decor", "cushions", "cushion covers", "throw pillows", "curtains",
                "window curtains", "bed sheet", "bedsheet", "bed cover", "quilt", "blanket",
                "comforter", "duvet", "rug", "carpets", "floor mat", "doormat", "wall art",
                "paintings", "wall hanging", "wall clock", "photo frame", "vases", "flower vase",
                "candles", "scented candles", "aroma diffuser", "essential oils", "fairy lights",
                "table lamp", "floor lamp", "lamps", "mirror", "wall mirror"
            )
        ),
        SubcategoryItemEntry(
            category = "Home",
            subcategory = "Cleaning",
            items = listOf(
                "cleaning", "housekeeping", "detergent", "washing powder", "laundry detergent",
                "surf excel", "ariel", "tide", "rin", "liquid detergent", "fabric conditioner",
                "comfort", "dishwash", "dishwashing liquid", "vim", "vim gel", "pril",
                "dishwasher tablets", "floor cleaner", "lizol", "phenyl", "toilet cleaner",
                "harpic", "glass cleaner", "colin", "bathroom cleaner", "drain cleaner",
                "drainex", "mop", "spin mop", "floor wiper", "broom", "jhadu", "pocha",
                "dustpan", "microfiber cloth", "sponge", "scotch brite", "scrubber",
                "toilet brush", "plunger", "garbage can", "dustbin", "bucket"
            )
        ),
        SubcategoryItemEntry(
            category = "Home",
            subcategory = "Upkeep",
            items = listOf(
                "home upkeep", "home maintenance", "light repair", "faucet", "tap", "pipe replacement",
                "door lock", "padlock", "godrej lock", "doorknob", "door handle", "latch",
                "hinges", "curtain rod", "sealant", "m seal", "wd-40", "lubricant"
            )
        ),
        SubcategoryItemEntry(
            category = "Home",
            subcategory = "Painting",
            items = listOf(
                "painting home", "wall paint", "asian paints", "berger paints", "nerolac",
                "emulsion", "primer", "wall putty", "birla white", "distemper", "paint brush",
                "paint roller", "thinner", "sandpaper", "waterproofing", "damp proof"
            )
        ),
        SubcategoryItemEntry(
            category = "Home",
            subcategory = "Renovation",
            items = listOf(
                "renovation", "home renovation", "remodeling", "bathroom remodel",
                "kitchen remodel", "tiles", "vitrified tiles", "ceramic tiles", "granite",
                "marble", "counter top", "false ceiling", "pop ceiling", "wooden flooring",
                "modular kitchen", "chimney", "kitchen sink", "sanitaryware", "commode",
                "wash basin", "jaquar", "hindware", "sliding window", "glass partition"
            )
        ),
        SubcategoryItemEntry(
            category = "Home",
            subcategory = "Pest-control",
            items = listOf(
                "pest control", "pest management", "termite treatment", "cockroach control",
                "cockroach gel", "hit gel", "bedbug treatment", "rodent control", "rat trap",
                "rat poison", "mosquito repellent", "all out", "good knight", "good knight refill",
                "mortein", "mosquito bat", "hit spray", "red hit", "black hit", "odomos",
                "ant chalk", "laxman rekha", "fumigation"
            )
        ),
        SubcategoryItemEntry(
            category = "Home",
            subcategory = "Construction",
            items = listOf(
                "construction", "civil work", "building materials", "cement", "ultratech cement",
                "acc cement", "ambuja", "bricks", "red bricks", "concrete blocks", "sand",
                "gravel", "steel bars", "tmt steel", "tata tiscon", "labor charge", "contractor",
                "mason charge", "ready mix concrete"
            )
        ),

        // ==========================================
        // 6. ENTERTAINMENT
        // ==========================================
        SubcategoryItemEntry(
            category = "Entertainment",
            subcategory = "Movies",
            items = listOf(
                "movies", "movie", "film", "cinema", "movie ticket", "cinema ticket",
                "multiplex", "pvr", "pvr inox", "inox", "cinepolis", "bookmyshow", "bms",
                "imax", "imax 3d", "4dx", "movie popcorn", "multiplex food", "recliner ticket"
            )
        ),
        SubcategoryItemEntry(
            category = "Entertainment",
            subcategory = "Shows",
            items = listOf(
                "shows", "live show", "comedy show", "stand up comedy", "standup", "comic show",
                "concert", "live music concert", "gig", "live band", "theatre play", "stage play",
                "drama", "musical", "music festival", "edm concert", "sunburn", "lollapalooza"
            )
        ),
        SubcategoryItemEntry(
            category = "Entertainment",
            subcategory = "Bowling",
            items = listOf(
                "bowling", "bowling alley", "bowling game", "amoeba", "timezone", "smaash",
                "arcade", "arcade games", "laser tag", "paintball", "snooker", "pool table",
                "billiards", "virtual reality", "vr games", "escape room", "go karting"
            )
        ),
        SubcategoryItemEntry(
            category = "Entertainment",
            subcategory = "Tickets",
            items = listOf(
                "tickets", "amusement park", "theme park", "water park", "imagica", "wonderla",
                "museum", "museum ticket", "science center", "planetarium", "art gallery",
                "zoo", "zoo ticket", "safari ticket", "aquarium ticket", "monument ticket",
                "taj mahal ticket", "event pass", "entry fee", "expo ticket"
            )
        ),

        // ==========================================
        // 7. EVENTS
        // ==========================================
        SubcategoryItemEntry(
            category = "Events",
            subcategory = "Party",
            items = listOf(
                "party", "night out", "clubbing", "nightclub", "pub", "discotheque", "house party",
                "celebration", "bachelor party", "bachelorette", "farewell party", "reunion",
                "get together", "drinks party", "dj party", "cover charge", "stag entry",
                "party venue"
            )
        ),
        SubcategoryItemEntry(
            category = "Events",
            subcategory = "Birthday",
            items = listOf(
                "birthday", "birthday party", "bday", "birthday celebration", "birthday cake",
                "photo cake", "birthday gift", "birthday decor", "balloons", "party caps",
                "party popper", "return gifts", "party supplies"
            )
        ),
        SubcategoryItemEntry(
            category = "Events",
            subcategory = "Spiritual",
            items = listOf(
                "spiritual", "pooja", "puja", "religious ceremony", "havan", "satyanarayan pooja",
                "griha pravesh", "housewarming ceremony", "mundan", "naming ceremony",
                "temple offering", "temple donation", "hundi", "prasad", "dakshina",
                "pandit fee", "priest fee", "church offering", "gurudwara donation"
            )
        ),
        SubcategoryItemEntry(
            category = "Events",
            subcategory = "Wedding",
            items = listOf(
                "wedding", "marriage", "shaadi", "vivaah", "wedding celebration", "reception",
                "wedding reception", "sangeet", "mehendi", "haldi ceremony", "engagement",
                "roka", "banquet hall", "wedding caterer", "wedding photographer",
                "bridal makeup", "bridal lehenga", "wedding invitation", "wedding cards"
            )
        ),

        // ==========================================
        // 8. TRAVEL
        // ==========================================
        SubcategoryItemEntry(
            category = "Travel",
            subcategory = "Activities",
            items = listOf(
                "travel activities", "sightseeing", "city tour", "guided tour", "scuba diving",
                "snorkeling", "trekking", "hiking", "mountain trek", "river rafting",
                "bungee jumping", "paragliding", "parasailing", "wildlife safari", "safari",
                "desert safari", "camel ride", "hot air balloon", "zip lining", "boat ride",
                "cruise", "ferry ticket"
            )
        ),
        SubcategoryItemEntry(
            category = "Travel",
            subcategory = "Camping",
            items = listOf(
                "camping", "campsite", "outdoor camping", "glamping", "tent rental", "dome tent",
                "bonfire", "camp night", "sleeping bag", "camping gear"
            )
        ),
        SubcategoryItemEntry(
            category = "Travel",
            subcategory = "Hotel",
            items = listOf(
                "hotel", "hotel stay", "hotel room", "resort", "luxury resort", "beach resort",
                "lodge", "guest house", "room booking", "taj hotels", "marriott", "hyatt",
                "radisson", "staycation", "room service", "check in"
            )
        ),
        SubcategoryItemEntry(
            category = "Travel",
            subcategory = "Commute",
            items = listOf(
                "travel commute", "local sightseeing", "tourist taxi", "tempo traveller",
                "tourist bus", "airport shuttle", "island ferry", "intercity transfer"
            )
        ),
        SubcategoryItemEntry(
            category = "Travel",
            subcategory = "Visa fees",
            items = listOf(
                "visa", "visa application", "visa fee", "tourist visa", "evisa", "vfs",
                "vfs global", "embassy fee", "passport", "passport renewal", "tatkal passport",
                "travel insurance", "international permit"
            )
        ),
        SubcategoryItemEntry(
            category = "Travel",
            subcategory = "Hostel",
            items = listOf(
                "hostel", "backpacker hostel", "youth hostel", "dorm", "dormitory", "bunk bed",
                "zostel", "gostops", "the hosteller", "backpacker stay"
            )
        ),
        SubcategoryItemEntry(
            category = "Travel",
            subcategory = "Airbnb",
            items = listOf(
                "airbnb", "vrbo", "homestay", "vacation rental", "villa", "private villa",
                "pool villa", "farm stay", "airbnb booking"
            )
        ),
        SubcategoryItemEntry(
            category = "Travel",
            subcategory = "Oyo",
            items = listOf(
                "oyo", "oyo rooms", "oyo hotel", "oyo townhouse", "budget hotel room", "budget lodge"
            )
        ),

        // ==========================================
        // 9. MEDICAL
        // ==========================================
        SubcategoryItemEntry(
            category = "Medical",
            subcategory = "Medicines",
            items = listOf(
                "medicines", "medicine", "pharma", "tablets", "pills", "capsules", "syrup",
                "painkiller", "paracetamol", "dolo", "dolo 650", "crocin", "calpol",
                "combiflam", "aspirin", "antibiotics", "azithromycin", "amoxicillin",
                "antacid", "digene", "gelusil", "pan d", "cough syrup", "benadryl",
                "cetirizine", "allegra", "vitamins", "multivitamin", "becosules", "vitamin c",
                "limcee", "vitamin d", "zincovit", "calcium tablets", "inhaler", "insulin",
                "glucometer strips", "bandaid", "bandage", "crepe bandage", "betadine",
                "burnol", "moov", "volini", "iodex", "vicks", "thermometer", "bp monitor",
                "1mg", "apollo pharmacy", "pharmeasy", "medplus", "netmeds", "chemist"
            )
        ),
        SubcategoryItemEntry(
            category = "Medical",
            subcategory = "Hospital",
            items = listOf(
                "hospital", "hospital admission", "inpatient", "outpatient", "opd", "ipd",
                "hospital bill", "emergency room", "casualty", "icu", "operation",
                "surgery", "surgical procedure", "apollo hospital", "fortis", "max healthcare",
                "manipal hospital", "narayana health", "medanta", "aiims"
            )
        ),
        SubcategoryItemEntry(
            category = "Medical",
            subcategory = "Clinic",
            items = listOf(
                "clinic", "doctor", "physician", "general physician", "doctor consultation",
                "consultation fee", "specialist", "cardiologist", "dermatologist",
                "skin specialist", "pediatrician", "gynecologist", "orthopedist", "practo"
            )
        ),
        SubcategoryItemEntry(
            category = "Medical",
            subcategory = "Dentist",
            items = listOf(
                "dentist", "dental", "dental clinic", "teeth cleaning", "scaling",
                "tooth extraction", "root canal", "root canal treatment", "rct",
                "dental filling", "cavity", "dental crown", "braces", "aligners",
                "invisalign", "dentures", "wisdom tooth", "gum treatment"
            )
        ),
        SubcategoryItemEntry(
            category = "Medical",
            subcategory = "Lab test",
            items = listOf(
                "lab test", "medical test", "pathology", "blood test", "cbc", "lipid profile",
                "blood sugar", "hba1c", "thyroid test", "tsh", "lft", "kft", "urine test",
                "vitamin d test", "dr lal pathlabs", "lal pathlabs", "thyrocare", "metropolis",
                "x-ray", "xray", "ultrasound", "usg", "ct scan", "mri", "mri scan", "ecg"
            )
        ),
        SubcategoryItemEntry(
            category = "Medical",
            subcategory = "Hygiene",
            items = listOf(
                "medical hygiene", "sanitary pads", "sanitary napkins", "whisper", "stayfree",
                "tampons", "menstrual cup", "adult diapers", "masks", "n95 mask", "surgical mask",
                "hand sanitizer", "sanitiser", "dettol", "medical gloves", "sterile cotton",
                "antiseptic liquid"
            )
        ),

        // ==========================================
        // 10. PERSONAL
        // ==========================================
        SubcategoryItemEntry(
            category = "Personal",
            subcategory = "Self-care",
            items = listOf(
                "self care", "wellness", "spa", "body spa", "massage", "body massage",
                "thai massage", "head massage", "foot massage", "reflexology", "aromatherapy",
                "body scrub", "sauna", "steam bath", "jacuzzi", "bath salts", "essential oils"
            )
        ),
        SubcategoryItemEntry(
            category = "Personal",
            subcategory = "Grooming",
            items = listOf(
                "grooming", "salon", "parlor", "parlour", "beauty salon", "barber", "barbershop",
                "haircut", "hair trim", "hair styling", "hair color", "hair dye", "hair spa",
                "keratin treatment", "beard trim", "beard grooming", "shaving", "facial",
                "cleanup", "manicure", "pedicure", "nail art", "waxing", "threading",
                "urban company salon", "enrich salon", "jawed habib", "naturals salon"
            )
        ),
        SubcategoryItemEntry(
            category = "Personal",
            subcategory = "Hobbies",
            items = listOf(
                "hobbies", "hobby", "musical instruments", "guitar", "piano", "keyboard",
                "violin", "flute", "ukulele", "art supplies", "painting canvas", "calligraphy",
                "pottery", "knitting", "crochet", "embroidery", "sewing"
            )
        ),
        SubcategoryItemEntry(
            category = "Personal",
            subcategory = "Vices",
            items = listOf(
                "vices", "smoking", "cigarettes", "cigarette", "gold flake", "classic",
                "marlboro", "lighter", "bidi", "cigar", "rolling paper", "vape", "vaping",
                "e-cigarette", "tobacco", "pan masala", "paan", "hookah"
            )
        ),
        SubcategoryItemEntry(
            category = "Personal",
            subcategory = "Therapy",
            items = listOf(
                "therapy", "counseling", "counselling", "therapist", "counselor", "psychologist",
                "psychiatrist", "mental health", "cbt", "therapy session fee", "betterhelp"
            )
        ),

        // ==========================================
        // 11. FITNESS
        // ==========================================
        SubcategoryItemEntry(
            category = "Fitness",
            subcategory = "Gym",
            items = listOf(
                "gym", "gymnasium", "fitness center", "gym membership", "cult", "cult fit",
                "cult pass", "golds gym", "gold's gym", "anytime fitness", "crossfit",
                "personal trainer", "pt fee", "workout"
            )
        ),
        SubcategoryItemEntry(
            category = "Fitness",
            subcategory = "Badminton",
            items = listOf(
                "badminton", "badminton court", "court booking", "playo badminton",
                "badminton racket", "yonex", "lining", "shuttlecock", "feather shuttle",
                "nylon shuttle", "badminton shoes", "racket gutting"
            )
        ),
        SubcategoryItemEntry(
            category = "Fitness",
            subcategory = "Football",
            items = listOf(
                "football", "soccer", "turf", "turf booking", "playo football",
                "football match", "football boots", "studs", "football jersey", "shin guards"
            )
        ),
        SubcategoryItemEntry(
            category = "Fitness",
            subcategory = "Cricket",
            items = listOf(
                "cricket", "cricket match", "cricket turf", "box cricket", "cricket nets",
                "cricket bat", "english willow", "tennis cricket bat", "leather ball",
                "tennis ball", "batting gloves", "cricket kit"
            )
        ),
        SubcategoryItemEntry(
            category = "Fitness",
            subcategory = "Classes",
            items = listOf(
                "fitness classes", "yoga", "yoga class", "pilates", "reformer pilates",
                "zumba", "aerobics", "spinning", "spin class", "martial arts", "karate",
                "taekwondo", "boxing", "kickboxing", "mma", "swimming", "swimming classes",
                "dance class"
            )
        ),
        SubcategoryItemEntry(
            category = "Fitness",
            subcategory = "Equipment",
            items = listOf(
                "gym equipment", "fitness equipment", "dumbbells", "barbell", "weight plates",
                "kettlebell", "resistance bands", "pull up bar", "push up bars", "yoga mat",
                "skipping rope", "jump rope", "foam roller", "gym bench", "treadmill",
                "exercise cycle", "gym gloves", "shaker bottle", "gym bag"
            )
        ),
        SubcategoryItemEntry(
            category = "Fitness",
            subcategory = "Nutrition",
            items = listOf(
                "fitness nutrition", "protein", "whey protein", "whey isolate", "optimum nutrition",
                "on whey", "muscleblaze", "myprotein", "mass gainer", "creatine",
                "creatine monohydrate", "bcaa", "pre-workout", "pre workout", "protein bar",
                "peanut butter fitness", "fish oil capsules"
            )
        ),

        // ==========================================
        // 12. SERVICES
        // ==========================================
        SubcategoryItemEntry(
            category = "Services",
            subcategory = "Laundry",
            items = listOf(
                "laundry", "wash and fold", "wash and iron", "dry cleaning", "dry clean",
                "steam press", "ironing", "press", "laundromat", "wash clothes", "uclean"
            )
        ),
        SubcategoryItemEntry(
            category = "Services",
            subcategory = "Tailor",
            items = listOf(
                "tailor", "tailoring", "tailor shop", "stitching", "blouse stitching",
                "suit stitching", "pant alteration", "trouser alteration", "shirt alteration",
                "hemming", "darzi"
            )
        ),
        SubcategoryItemEntry(
            category = "Services",
            subcategory = "Courier",
            items = listOf(
                "courier", "parcel", "package delivery", "postage", "speed post", "indian post",
                "dhl", "fedex", "bluedart", "blue dart", "dtdc", "delhivery", "shadowfax",
                "shipping fee", "courier charges"
            )
        ),
        SubcategoryItemEntry(
            category = "Services",
            subcategory = "Carpenter",
            items = listOf(
                "carpenter", "carpentry", "wood work", "wooden repair", "door repair",
                "furniture assembly", "bed assembly", "wood polish", "carpenter labor"
            )
        ),
        SubcategoryItemEntry(
            category = "Services",
            subcategory = "Plumber",
            items = listOf(
                "plumber", "plumbing", "pipe leak", "leaking tap", "faucet repair",
                "drain unclog", "toilet clog", "flush repair", "pipe fitting", "pump repair",
                "plumber labor"
            )
        ),
        SubcategoryItemEntry(
            category = "Services",
            subcategory = "Mechanic",
            items = listOf(
                "mechanic", "car mechanic", "bike mechanic", "garage", "service center",
                "car service", "bike service", "oil change", "engine oil", "brake repair",
                "battery replacement", "puncture", "tyre puncture", "wheel alignment",
                "roadside assistance", "towing"
            )
        ),
        SubcategoryItemEntry(
            category = "Services",
            subcategory = "Photographer",
            items = listOf(
                "photographer", "photography", "photoshoot", "photo shoot", "portrait",
                "event photography", "photo studio", "passport size photo", "photo album",
                "videographer"
            )
        ),
        SubcategoryItemEntry(
            category = "Services",
            subcategory = "Driver",
            items = listOf(
                "driver", "personal driver", "driver salary", "chauffeur", "call driver",
                "acting driver", "valet driver", "hire a driver", "driveu"
            )
        ),
        SubcategoryItemEntry(
            category = "Services",
            subcategory = "Vehicle Wash",
            items = listOf(
                "vehicle wash", "car wash", "bike wash", "pressure wash", "foam wash",
                "car vacuuming", "car detailing", "ceramic coating", "teflon coating"
            )
        ),
        SubcategoryItemEntry(
            category = "Services",
            subcategory = "Electrician",
            items = listOf(
                "electrician", "electrical work", "electrical repair", "wiring", "short circuit",
                "switch replacement", "socket repair", "ceiling fan installation", "fan repair",
                "light fitting", "inverter repair", "electrician labor"
            )
        ),
        SubcategoryItemEntry(
            category = "Services",
            subcategory = "Painting",
            items = listOf(
                "house painter", "painter labor", "wall painting work", "whitewash",
                "putty work", "painter daily wage", "painting contractor"
            )
        ),
        SubcategoryItemEntry(
            category = "Services",
            subcategory = "Xerox",
            items = listOf(
                "xerox", "photocopy", "printout", "document print", "color print",
                "scanning", "lamination", "spiral binding", "hard binding"
            )
        ),
        SubcategoryItemEntry(
            category = "Services",
            subcategory = "Legal",
            items = listOf(
                "legal", "lawyer", "advocate", "attorney", "legal fee", "court fee",
                "stamp duty", "stamp paper", "notary", "notarization", "affidavit",
                "power of attorney", "rent agreement", "sale deed registration"
            )
        ),
        SubcategoryItemEntry(
            category = "Services",
            subcategory = "Advisor",
            items = listOf(
                "advisor", "advisory", "consultant", "consultancy fee", "chartered accountant",
                "ca fee", "tax consultant", "itr filing", "financial advisor", "financial planner"
            )
        ),
        SubcategoryItemEntry(
            category = "Services",
            subcategory = "Repair",
            items = listOf(
                "repair", "technician", "phone repair", "screen repair", "display replacement",
                "laptop repair", "appliance repair", "fridge repair", "washing machine repair",
                "tv repair", "ac servicing", "urban company repair"
            )
        ),
        SubcategoryItemEntry(
            category = "Services",
            subcategory = "Logistics",
            items = listOf(
                "logistics", "packers and movers", "movers and packers", "shifting",
                "house shifting", "tempo", "tata ace", "porter", "porter app", "truck rental",
                "loading labor", "packing service", "freight"
            )
        ),

        // ==========================================
        // 13. BILL
        // ==========================================
        SubcategoryItemEntry(
            category = "Bill",
            subcategory = "Phone",
            items = listOf(
                "phone", "mobile", "mobile bill", "mobile recharge", "prepaid recharge",
                "postpaid bill", "airtel", "airtel recharge", "jio", "jio prepaid",
                "jio postpaid", "vi", "vodafone idea", "bsnl", "sim recharge", "data pack"
            )
        ),
        SubcategoryItemEntry(
            category = "Bill",
            subcategory = "Rent",
            items = listOf(
                "rent", "house rent", "room rent", "flat rent", "apartment rent", "pg",
                "pg rent", "paying guest", "coliving", "stanza living", "zolo", "office rent",
                "shop rent", "monthly rent", "landlord rent"
            )
        ),
        SubcategoryItemEntry(
            category = "Bill",
            subcategory = "Water",
            items = listOf(
                "water", "water bill", "municipal water", "jal board", "delhi jal board",
                "bwssb", "water supply", "drinking water", "water tanker", "bisleri can",
                "water can delivery"
            )
        ),
        SubcategoryItemEntry(
            category = "Bill",
            subcategory = "Electricity",
            items = listOf(
                "electricity", "electric bill", "power", "power bill", "electricity board",
                "bijli bill", "bescom", "mseb", "mahavitaran", "tneb", "cesc", "tata power",
                "adani electricity", "uppcl", "bses", "prepaid meter recharge"
            )
        ),
        SubcategoryItemEntry(
            category = "Bill",
            subcategory = "Gas",
            items = listOf(
                "gas", "gas bill", "piped gas", "png", "mgl", "mahanagar gas", "igl",
                "cooking gas", "lpg", "lpg cylinder", "cylinder refill", "gas cylinder",
                "indane", "bharat gas", "hp gas"
            )
        ),
        SubcategoryItemEntry(
            category = "Bill",
            subcategory = "Internet",
            items = listOf(
                "internet", "wifi", "wi-fi", "broadband", "fiber", "optical fiber",
                "broadband bill", "wifi bill", "router bill", "act fibernet", "jio fiber",
                "airtel xtreme", "airtel broadband", "bsnl broadband", "hathway", "excitel"
            )
        ),
        SubcategoryItemEntry(
            category = "Bill",
            subcategory = "House Help",
            items = listOf(
                "house help", "maid", "maid salary", "kaamwali bai", "cleaning lady",
                "sweeper", "sweeper salary", "gardener", "mali", "watchman", "security guard",
                "security salary", "ironing person"
            )
        ),
        SubcategoryItemEntry(
            category = "Bill",
            subcategory = "Education",
            items = listOf(
                "education", "school fee", "college fee", "tuition", "tuition fee", "coaching",
                "coaching class", "allen", "aakash", "physics wallah", "unacademy",
                "semester fee", "exam fee", "admission fee", "school bus fee"
            )
        ),
        SubcategoryItemEntry(
            category = "Bill",
            subcategory = "DTH",
            items = listOf(
                "dth", "direct to home", "tv recharge", "cable tv", "set top box",
                "tata play", "tata sky", "airtel dth", "airtel digital tv", "dish tv",
                "d2h", "sun direct"
            )
        ),
        SubcategoryItemEntry(
            category = "Bill",
            subcategory = "Cook",
            items = listOf(
                "cook", "chef", "domestic cook", "cook salary", "maharaj", "kitchen help",
                "private chef", "cooking maid"
            )
        ),
        SubcategoryItemEntry(
            category = "Bill",
            subcategory = "Maintenance",
            items = listOf(
                "maintenance", "society maintenance", "apartment maintenance", "rwa",
                "rwa fee", "building maintenance", "hoa fee", "flat maintenance", "lift maintenance"
            )
        ),

        // ==========================================
        // 14. SUBSCRIPTION
        // ==========================================
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Software",
            items = listOf(
                "software", "cloud storage", "icloud", "icloud plus", "google one",
                "google drive", "dropbox", "onedrive", "microsoft 365", "office 365",
                "adobe", "photoshop", "figma", "canva", "canva pro", "notion", "github",
                "github copilot", "slack", "zoom", "vpn", "nordvpn", "domain renewal", "hosting"
            )
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "News",
            items = listOf(
                "news", "newspaper", "digital news", "the hindu", "times of india",
                "indian express", "the ken", "mint", "livemint", "economic times", "et prime",
                "new york times", "wall street journal", "wsj", "economist", "medium", "substack"
            )
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Netflix",
            items = listOf("netflix", "netflix subscription", "netflix monthly", "netflix 4k")
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Prime",
            items = listOf("prime", "amazon prime", "prime video", "prime membership")
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Youtube",
            items = listOf("youtube", "youtube premium", "yt premium")
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Youtube Music",
            items = listOf("youtube music", "yt music", "yt music premium")
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Spotify",
            items = listOf("spotify", "spotify premium", "spotify individual", "spotify family")
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Google",
            items = listOf("google workspace", "g suite", "google storage", "google developer")
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Learning",
            items = listOf(
                "learning", "duolingo", "duolingo super", "masterclass", "skillshare",
                "brilliant", "coursera plus", "codecademy", "leetcode", "leetcode premium",
                "chess com", "blinkist"
            )
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Apple Tv",
            items = listOf("apple tv", "apple tv plus", "apple tv+")
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Apple Music",
            items = listOf("apple music", "apple one", "apple one bundle")
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Bumble",
            items = listOf(
                "bumble", "bumble boost", "bumble premium", "tinder", "tinder gold",
                "hinge", "hinge plus", "shaadi com", "matrimony"
            )
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "JioCinema",
            items = listOf("jiocinema", "jio cinema", "hotstar", "disney hotstar")
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Google Play",
            items = listOf("google play", "google play pass", "play store subscription")
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Xbox",
            items = listOf("xbox", "xbox game pass", "game pass ultimate", "pc game pass")
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "PlayStation",
            items = listOf("playstation", "ps plus", "playstation plus", "psn")
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Disney Plus",
            items = listOf("disney plus", "disney+", "hulu", "espn+")
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Zee5",
            items = listOf("zee5", "zee5 premium", "sonyliv", "sony liv", "aha video", "sun nxt")
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "ChatGPT",
            items = listOf("chatgpt", "chat gpt", "openai", "chatgpt plus", "gpt-4", "gpt plus")
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Claude",
            items = listOf("claude", "claude ai", "anthropic", "claude pro")
        ),
        SubcategoryItemEntry(
            category = "Subscription",
            subcategory = "Grok",
            items = listOf("grok", "x premium", "twitter blue", "xai")
        ),

        // ==========================================
        // 15. EMI
        // ==========================================
        SubcategoryItemEntry(
            category = "EMI",
            subcategory = "Electronics",
            items = listOf(
                "electronics emi", "phone emi", "iphone emi", "laptop emi", "macbook emi",
                "tv emi", "no cost emi", "bajaj finserv emi", "consumer durable loan"
            )
        ),
        SubcategoryItemEntry(
            category = "EMI",
            subcategory = "House",
            items = listOf(
                "home loan", "housing loan", "home emi", "housing loan emi", "mortgage",
                "sbi home loan", "hdfc home loan", "lic housing finance"
            )
        ),
        SubcategoryItemEntry(
            category = "EMI",
            subcategory = "Vehicle",
            items = listOf(
                "car loan", "car emi", "vehicle loan", "auto loan", "bike loan",
                "two wheeler loan", "scooter emi", "bike emi"
            )
        ),
        SubcategoryItemEntry(
            category = "EMI",
            subcategory = "Education",
            items = listOf(
                "education loan", "student loan", "study loan", "college loan",
                "education loan installment", "hdfc credila"
            )
        ),

        // ==========================================
        // 16. CREDIT BILL
        // ==========================================
        SubcategoryItemEntry(
            category = "Credit Bill",
            subcategory = "Credit Card",
            items = listOf(
                "credit card", "credit card bill", "credit card payment", "card dues",
                "hdfc credit card", "sbi card", "icici credit card", "axis credit card",
                "amex", "american express", "onecard", "cred", "cred bill"
            )
        ),
        SubcategoryItemEntry(
            category = "Credit Bill",
            subcategory = "Simpl",
            items = listOf("simpl", "simpl bill", "simpl pay later", "getsimpl")
        ),
        SubcategoryItemEntry(
            category = "Credit Bill",
            subcategory = "Slice",
            items = listOf("slice", "slice card", "slice borrow", "slice credit")
        ),
        SubcategoryItemEntry(
            category = "Credit Bill",
            subcategory = "lazypay",
            items = listOf("lazypay", "lazy pay", "paylater", "lazypay bill")
        ),
        SubcategoryItemEntry(
            category = "Credit Bill",
            subcategory = "Amazon Pay",
            items = listOf("amazon pay later", "amazon pay later bill", "amazon credit card")
        ),

        // ==========================================
        // 17. INVESTMENT
        // ==========================================
        SubcategoryItemEntry(
            category = "Investment",
            subcategory = "Mutual Funds",
            items = listOf(
                "mutual funds", "mf", "sip", "systematic investment plan", "index fund",
                "nifty 50", "sensex", "elss", "tax saver fund", "parag parikh", "groww",
                "zerodha coin", "coin", "kuvera", "et money"
            )
        ),
        SubcategoryItemEntry(
            category = "Investment",
            subcategory = "Stocks",
            items = listOf(
                "stocks", "shares", "equity", "stock market", "share market", "demat",
                "trading", "zerodha", "kite", "upstox", "angel one", "groww stocks"
            )
        ),
        SubcategoryItemEntry(
            category = "Investment",
            subcategory = "IPO",
            items = listOf("ipo", "initial public offering", "ipo application", "ipo bid", "asba")
        ),
        SubcategoryItemEntry(
            category = "Investment",
            subcategory = "PPF",
            items = listOf("ppf", "public provident fund", "epf", "vpf", "nps", "pension fund")
        ),
        SubcategoryItemEntry(
            category = "Investment",
            subcategory = "Fixed Deposit",
            items = listOf("fixed deposit", "fd", "term deposit", "bank fd", "sbi fd", "hdfc fd")
        ),
        SubcategoryItemEntry(
            category = "Investment",
            subcategory = "Recurring Deposit",
            items = listOf("recurring deposit", "rd", "monthly rd", "post office rd")
        ),
        SubcategoryItemEntry(
            category = "Investment",
            subcategory = "Assets",
            items = listOf("assets", "real estate", "land", "plot purchase", "property investment", "reit")
        ),
        SubcategoryItemEntry(
            category = "Investment",
            subcategory = "Crypto",
            items = listOf(
                "crypto", "cryptocurrency", "bitcoin", "btc", "ethereum", "eth", "solana",
                "wazirx", "coinswitch", "coindcx", "binance"
            )
        ),
        SubcategoryItemEntry(
            category = "Investment",
            subcategory = "Gold",
            items = listOf(
                "gold", "digital gold", "sovereign gold bond", "sgb", "gold etf", "gold coin",
                "gold bar", "silver", "silver etf"
            )
        ),

        // ==========================================
        // 18. SUPPORT
        // ==========================================
        SubcategoryItemEntry(
            category = "Support",
            subcategory = "Parents",
            items = listOf("parents", "parents support", "money to parents", "sending money home")
        ),
        SubcategoryItemEntry(
            category = "Support",
            subcategory = "Spouse",
            items = listOf("spouse", "wife", "husband", "partner", "wife allowance", "husband money")
        ),
        SubcategoryItemEntry(
            category = "Support",
            subcategory = "Mom",
            items = listOf("mom", "mother", "mummy", "maa", "mother expenses")
        ),
        SubcategoryItemEntry(
            category = "Support",
            subcategory = "Dad",
            items = listOf("dad", "father", "papa", "pitaji", "father expenses")
        ),
        SubcategoryItemEntry(
            category = "Support",
            subcategory = "Pocket Money",
            items = listOf("pocket money", "allowance", "sibling", "brother", "sister", "cousin")
        ),

        // ==========================================
        // 19. INSURANCE
        // ==========================================
        SubcategoryItemEntry(
            category = "Insurance",
            subcategory = "Health",
            items = listOf(
                "health insurance", "mediclaim", "medical insurance", "star health",
                "hdfc ergo", "care health", "niva bupa", "family floater"
            )
        ),
        SubcategoryItemEntry(
            category = "Insurance",
            subcategory = "Vehicle",
            items = listOf(
                "vehicle insurance", "car insurance", "bike insurance", "motor insurance",
                "acko", "digit insurance", "policybazaar"
            )
        ),
        SubcategoryItemEntry(
            category = "Insurance",
            subcategory = "Life",
            items = listOf(
                "life insurance", "term insurance", "term life", "lic", "lic premium",
                "hdfc life", "max life"
            )
        ),
        SubcategoryItemEntry(
            category = "Insurance",
            subcategory = "Electronics",
            items = listOf(
                "electronics insurance", "gadget insurance", "mobile insurance",
                "applecare", "applecare plus", "extended warranty"
            )
        ),

        // ==========================================
        // 20. TAX
        // ==========================================
        SubcategoryItemEntry(
            category = "Tax",
            subcategory = "Income Tax",
            items = listOf("income tax", "advance tax", "self assessment tax", "itr", "tds", "challan 280")
        ),
        SubcategoryItemEntry(
            category = "Tax",
            subcategory = "GST",
            items = listOf("gst", "goods and services tax", "gst payment", "gstr 3b", "input tax credit")
        ),
        SubcategoryItemEntry(
            category = "Tax",
            subcategory = "Property Tax",
            items = listOf("property tax", "house tax", "municipal tax", "bbmp property tax", "water tax")
        ),

        // ==========================================
        // 21. TOP-UP
        // ==========================================
        SubcategoryItemEntry(
            category = "Top-up",
            subcategory = "UPI Lite",
            items = listOf("upi lite", "upi lite load", "upi lite recharge", "pinless upi")
        ),
        SubcategoryItemEntry(
            category = "Top-up",
            subcategory = "Paytm",
            items = listOf("paytm", "paytm wallet", "add money to paytm", "paytm topup")
        ),
        SubcategoryItemEntry(
            category = "Top-up",
            subcategory = "Amazon",
            items = listOf("amazon pay", "amazon pay balance", "amazon wallet", "add money to amazon")
        ),
        SubcategoryItemEntry(
            category = "Top-up",
            subcategory = "PhonePe",
            items = listOf("phonepe", "phonepe wallet", "phonepe balance", "phonepe topup")
        ),
        SubcategoryItemEntry(
            category = "Top-up",
            subcategory = "Google pay",
            items = listOf("google pay", "gpay wallet", "google balance", "google pay recharge")
        ),

        // ==========================================
        // 22. CHILDREN
        // ==========================================
        SubcategoryItemEntry(
            category = "Children",
            subcategory = "Nutrition",
            items = listOf(
                "baby food", "formula milk", "nan pro", "enfamil", "similac", "cerelac",
                "pediasure", "junior horlicks"
            )
        ),
        SubcategoryItemEntry(
            category = "Children",
            subcategory = "Necessities",
            items = listOf(
                "diapers", "pampers", "huggies", "baby wipes", "diaper rash cream",
                "baby powder", "baby soap", "baby lotion", "feeding bottle", "pram", "stroller"
            )
        ),
        SubcategoryItemEntry(
            category = "Children",
            subcategory = "Toys",
            items = listOf("kids toys", "rattles", "building blocks", "educational puzzles", "coloring books")
        ),
        SubcategoryItemEntry(
            category = "Children",
            subcategory = "Medical",
            items = listOf("pediatrician", "baby doctor", "baby vaccination", "polio drops", "immunization")
        ),
        SubcategoryItemEntry(
            category = "Children",
            subcategory = "Care",
            items = listOf("daycare", "creche", "babysitter", "nanny", "play school", "nursery school")
        ),
        SubcategoryItemEntry(
            category = "Children",
            subcategory = "Tuition Fee",
            items = listOf("tuition", "home tutor", "private tutor", "maths tuition", "science tuition")
        ),
        SubcategoryItemEntry(
            category = "Children",
            subcategory = "Classes Fee",
            items = listOf("kids classes", "hobby class", "swimming coaching", "abacus", "kumon")
        ),
        SubcategoryItemEntry(
            category = "Children",
            subcategory = "School Fee",
            items = listOf("school fee", "school fees", "admission fee school", "school books", "school uniform")
        ),
        SubcategoryItemEntry(
            category = "Children",
            subcategory = "College Fee",
            items = listOf("college fee", "semester fee", "university tuition", "hostel fee college")
        ),

        // ==========================================
        // 23. PET CARE
        // ==========================================
        SubcategoryItemEntry(
            category = "Pet Care",
            subcategory = "Food",
            items = listOf(
                "pet food", "dog food", "puppy food", "pedigree", "royal canin", "drools",
                "cat food", "whiskas", "pet treats", "dog treats", "chew bones", "catnip"
            )
        ),
        SubcategoryItemEntry(
            category = "Pet Care",
            subcategory = "Toys",
            items = listOf("pet toys", "dog toys", "chew toy", "squeaky toy", "cat toys", "pet bed", "leash", "collar")
        ),
        SubcategoryItemEntry(
            category = "Pet Care",
            subcategory = "Grooming",
            items = listOf("pet grooming", "dog grooming", "pet bath", "dog shampoo", "pet brush", "tick spray")
        ),
        SubcategoryItemEntry(
            category = "Pet Care",
            subcategory = "Vet",
            items = listOf(
                "vet", "veterinarian", "animal hospital", "vet consultation", "dog vaccination",
                "rabies vaccine", "deworming", "pet medicine"
            )
        ),

        // ==========================================
        // 24. BUSINESS
        // ==========================================
        SubcategoryItemEntry(
            category = "Business",
            subcategory = "Salary",
            items = listOf("staff salary", "employee salary", "payroll", "wages", "intern stipend")
        ),
        SubcategoryItemEntry(
            category = "Business",
            subcategory = "Inventory",
            items = listOf("inventory", "raw materials", "stock purchase", "wholesale", "supplier payment")
        ),
        SubcategoryItemEntry(
            category = "Business",
            subcategory = "Rent",
            items = listOf("office rent", "commercial rent", "shop rent", "warehouse rent", "coworking", "wework")
        ),
        SubcategoryItemEntry(
            category = "Business",
            subcategory = "Logistics",
            items = listOf("commercial freight", "cargo", "bulk shipping", "freight forwarder", "consignment")
        ),
        SubcategoryItemEntry(
            category = "Business",
            subcategory = "Software",
            items = listOf("business software", "saas", "salesforce", "hubspot", "zoho", "tally", "jira", "aws business")
        ),
        SubcategoryItemEntry(
            category = "Business",
            subcategory = "Marketing",
            items = listOf("marketing", "google ads", "meta ads", "facebook ads", "billboard", "brochures", "seo services")
        ),
        SubcategoryItemEntry(
            category = "Business",
            subcategory = "Tax",
            items = listOf("business tax", "corporate tax", "gst business", "professional tax", "trade license")
        ),
        SubcategoryItemEntry(
            category = "Business",
            subcategory = "Insurance",
            items = listOf("business insurance", "commercial insurance", "shop insurance", "fire insurance")
        ),
        SubcategoryItemEntry(
            category = "Business",
            subcategory = "Service",
            items = listOf("vendor payment", "business services", "facility management", "external auditor")
        ),

        // ==========================================
        // 25. MISCELLANEOUS
        // ==========================================
        SubcategoryItemEntry(
            category = "Miscellaneous",
            subcategory = "Tip",
            items = listOf("tip", "gratuity", "waiter tip", "valet tip", "delivery tip", "baksheesh")
        ),
        SubcategoryItemEntry(
            category = "Miscellaneous",
            subcategory = "Verification",
            items = listOf("verification charge", "micro transaction", "test transaction", "1 rupee charge")
        ),
        SubcategoryItemEntry(
            category = "Miscellaneous",
            subcategory = "Forex",
            items = listOf("forex", "foreign exchange", "currency exchange", "forex markup", "conversion fee")
        ),
        SubcategoryItemEntry(
            category = "Miscellaneous",
            subcategory = "Deposit",
            items = listOf("security deposit", "rental deposit", "caution deposit", "locker deposit")
        ),
        SubcategoryItemEntry(
            category = "Miscellaneous",
            subcategory = "Gift Cards",
            items = listOf("gift card", "gift voucher", "amazon voucher", "flipkart voucher", "shopping voucher")
        ),

        // ==========================================
        // 26 - 32. GENERAL CATEGORIES (No default subs)
        // ==========================================
        SubcategoryItemEntry(
            category = "Self Transfer",
            subcategory = "Self Transfer",
            items = listOf(
                "self transfer", "transfer", "self account", "own account", "bank to bank",
                "interbank transfer", "transfer to self", "moving money", "savings transfer",
                "fund transfer"
            )
        ),
        SubcategoryItemEntry(
            category = "Savings",
            subcategory = "Savings",
            items = listOf(
                "savings", "saving", "rainy day fund", "emergency fund", "piggy bank",
                "money saved", "goal savings", "future savings", "contingency fund"
            )
        ),
        SubcategoryItemEntry(
            category = "Gift",
            subcategory = "Gift",
            items = listOf(
                "gift", "gifts", "present", "presents", "birthday gift", "wedding gift",
                "anniversary present", "festival gift", "gift box", "gift hamper", "surprise"
            )
        ),
        SubcategoryItemEntry(
            category = "Lent",
            subcategory = "Lent",
            items = listOf(
                "lent", "money lent", "loan to friend", "friend borrowed", "money given",
                "pending return", "owed to me", "hand loan"
            )
        ),
        SubcategoryItemEntry(
            category = "Donation",
            subcategory = "Donation",
            items = listOf(
                "donation", "charity", "ngo", "donate", "contribution", "relief fund",
                "pm cares", "temple donation", "church tithe", "mosque donation", "zakat", "give india"
            )
        ),
        SubcategoryItemEntry(
            category = "Hidden Charges",
            subcategory = "Hidden Charges",
            items = listOf(
                "hidden charges", "bank charges", "annual charge", "maintenance fee",
                "debit card fee", "minimum balance charge", "sms alert charges",
                "atm decline fee", "overdraft fee", "late payment fee"
            )
        ),
        SubcategoryItemEntry(
            category = "Cash Withdrawal",
            subcategory = "Cash Withdrawal",
            items = listOf(
                "cash withdrawal", "cash", "atm", "atm cash", "atm withdrawal",
                "cash from bank", "debit card withdrawal", "withdrawn"
            )
        ),

        // ==========================================
        // 33. INCOME
        // ==========================================
        SubcategoryItemEntry(
            category = "Income",
            subcategory = "Salary",
            items = listOf(
                "salary", "monthly salary", "paycheck", "wage", "wages", "net pay",
                "direct deposit", "payroll", "stipend", "corporate salary"
            )
        ),
        SubcategoryItemEntry(
            category = "Income",
            subcategory = "Freelance",
            items = listOf(
                "freelance", "freelance income", "client payment", "contract work",
                "consulting fee", "gig earnings", "upwork", "fiverr", "project payout"
            )
        ),
        SubcategoryItemEntry(
            category = "Income",
            subcategory = "Business",
            items = listOf(
                "business income", "sales", "sales revenue", "revenue", "customer payment",
                "client receipt", "shop earnings", "store sales"
            )
        ),
        SubcategoryItemEntry(
            category = "Income",
            subcategory = "Bonus",
            items = listOf(
                "bonus", "annual bonus", "performance bonus", "festive bonus", "diwali bonus",
                "incentive", "appraisal", "profit sharing"
            )
        ),
        SubcategoryItemEntry(
            category = "Income",
            subcategory = "Gift",
            items = listOf("cash gift", "birthday money", "wedding cash", "monetary blessing", "shagun")
        ),
        SubcategoryItemEntry(
            category = "Income",
            subcategory = "Interest",
            items = listOf(
                "interest", "bank interest", "savings interest", "fd interest",
                "fixed deposit interest", "bond interest", "dividend", "stock dividend"
            )
        ),
        SubcategoryItemEntry(
            category = "Income",
            subcategory = "Refund",
            items = listOf(
                "refund", "cash back", "cashback", "credit refund", "amazon refund",
                "flipkart refund", "reimbursement", "office reimbursement", "claim"
            )
        ),
        SubcategoryItemEntry(
            category = "Income",
            subcategory = "Other",
            items = listOf(
                "other income", "rental income", "rent received", "tenant payment",
                "royalty", "scrap sale", "olx sale", "miscellaneous income"
            )
        )
    )

    // Pre-indexed lookup maps for ultra-fast matching
    private val subcategoryIndex: Map<String, List<SubcategoryItemEntry>>
    private val categoryIndex: Map<String, List<SubcategoryItemEntry>>
    private val compositeIndex: Map<String, SubcategoryItemEntry>
    private val categoryItemsMap: Map<String, Set<String>>

    init {
        val subMap = mutableMapOf<String, MutableList<SubcategoryItemEntry>>()
        val catMap = mutableMapOf<String, MutableList<SubcategoryItemEntry>>()
        val compMap = mutableMapOf<String, SubcategoryItemEntry>()
        val catItems = mutableMapOf<String, MutableSet<String>>()

        for (entry in ENTRIES) {
            val subKey = entry.subcategory.lowercase(Locale.ROOT)
            val catKey = entry.category.lowercase(Locale.ROOT)
            val compKey = "$catKey::$subKey"

            subMap.getOrPut(subKey) { mutableListOf() }.add(entry)
            catMap.getOrPut(catKey) { mutableListOf() }.add(entry)
            compMap[compKey] = entry

            catItems.getOrPut(catKey) { mutableSetOf() }.addAll(entry.items.map { it.lowercase(Locale.ROOT) })
        }

        subcategoryIndex = subMap
        categoryIndex = catMap
        compositeIndex = compMap
        categoryItemsMap = catItems
    }

    /**
     * Checks if a specific subcategory matches a search query via its item catalogue.
     */
    fun matchesSubcategory(categoryName: String, subcategoryName: String, query: String): Boolean {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isBlank()) return false

        val compKey = "${categoryName.lowercase(Locale.ROOT)}::${subcategoryName.lowercase(Locale.ROOT)}"
        val entry = compositeIndex[compKey] 
            ?: subcategoryIndex[subcategoryName.lowercase(Locale.ROOT)]?.firstOrNull()
            ?: return false

        return matchEntry(entry, q) != null
    }

    /**
     * Checks if a category matches a search query via its item catalogue.
     */
    fun matchesCategory(categoryName: String, query: String): Boolean {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isBlank()) return false

        val catKey = categoryName.lowercase(Locale.ROOT)
        val items = categoryItemsMap[catKey] ?: return false

        for (item in items) {
            if (isMatch(item, q)) return true
        }
        return false
    }

    /**
     * Returns the best matching item string for a subcategory given a query.
     */
    fun getMatchedItem(categoryName: String, subcategoryName: String, query: String): String? {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isBlank()) return null

        val compKey = "${categoryName.lowercase(Locale.ROOT)}::${subcategoryName.lowercase(Locale.ROOT)}"
        val entry = compositeIndex[compKey]
            ?: subcategoryIndex[subcategoryName.lowercase(Locale.ROOT)]?.firstOrNull()
            ?: return null

        return matchEntry(entry, q)?.first
    }

    /**
     * Gets all keywords/items associated with a subcategory.
     */
    fun getItemsForSubcategory(categoryName: String? = null, subcategoryName: String): Set<String> {
        val subKey = subcategoryName.lowercase(Locale.ROOT)
        if (categoryName != null) {
            val compKey = "${categoryName.lowercase(Locale.ROOT)}::$subKey"
            compositeIndex[compKey]?.let { return it.items.toSet() }
        }
        return subcategoryIndex[subKey]?.flatMap { it.items }?.toSet() ?: emptySet()
    }

    /**
     * Gets all keywords/items associated with a category.
     */
    fun getItemsForCategory(categoryName: String): Set<String> {
        return categoryItemsMap[categoryName.lowercase(Locale.ROOT)] ?: emptySet()
    }

    /**
     * Performs a smart ranked search across categories and subcategories using the catalogue.
     * Returns a list of [QuickSuggestionMatch] ordered by relevance.
     */
    fun searchSuggestions(
        categories: List<CategoryEntity>,
        subcategoriesMap: Map<Long, List<SubcategoryEntity>>,
        query: String,
        limit: Int = 12
    ): List<QuickSuggestionMatch> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isBlank()) return emptyList()

        val results = mutableListOf<QuickSuggestionMatch>()
        val seenKeys = mutableSetOf<String>()

        // 1. Score each subcategory
        for (category in categories) {
            val subs = subcategoriesMap[category.id] ?: emptyList()
            for (sub in subs) {
                val key = "${category.id}_${sub.id}"
                var bestScore = 0
                var matchedKeyword: String? = null

                val subNameLower = sub.name.lowercase(Locale.ROOT)

                // Direct subcategory name match
                if (subNameLower == q) {
                    bestScore = 100
                } else if (subNameLower.startsWith(q)) {
                    bestScore = 90
                } else if (subNameLower.split(" ", "-", "/").any { it.startsWith(q) }) {
                    bestScore = 85
                } else if (subNameLower.contains(q)) {
                    bestScore = 75
                }

                // Check catalogue items for this subcategory
                val compKey = "${category.name.lowercase(Locale.ROOT)}::$subNameLower"
                val entry = compositeIndex[compKey]
                    ?: subcategoryIndex[subNameLower]?.firstOrNull()

                if (entry != null) {
                    val matchResult = matchEntry(entry, q)
                    if (matchResult != null) {
                        val (item, itemScore) = matchResult
                        if (itemScore > bestScore) {
                            bestScore = itemScore
                            matchedKeyword = item
                        }
                    }
                }

                if (bestScore > 0 && seenKeys.add(key)) {
                    results.add(
                        QuickSuggestionMatch(
                            category = category,
                            subcategory = sub,
                            matchedItem = matchedKeyword,
                            score = bestScore
                        )
                    )
                }
            }
        }

        // 2. Score category-level matches (if not already included)
        for (category in categories) {
            val key = "${category.id}_null"
            if (seenKeys.contains(key)) continue

            val catNameLower = category.name.lowercase(Locale.ROOT)
            var catScore = 0
            var matchedKeyword: String? = null

            if (catNameLower == q) {
                catScore = 70
            } else if (catNameLower.startsWith(q)) {
                catScore = 65
            } else if (catNameLower.split(" ", "&", "/").any { it.startsWith(q) }) {
                catScore = 60
            } else if (catNameLower.contains(q)) {
                catScore = 50
            }

            // Check if any category-level item matched
            val catItems = categoryItemsMap[catNameLower]
            if (catItems != null) {
                for (item in catItems) {
                    val score = scoreItemMatch(item, q)
                    if (score > 0 && (score - 15) > catScore) {
                        catScore = score - 15
                        matchedKeyword = item
                    }
                }
            }

            if (catScore > 0 && seenKeys.add(key)) {
                results.add(
                    QuickSuggestionMatch(
                        category = category,
                        subcategory = null,
                        matchedItem = matchedKeyword,
                        score = catScore
                    )
                )
            }
        }

        return results.sortedByDescending { it.score }.take(limit)
    }

    private fun matchEntry(entry: SubcategoryItemEntry, q: String): Pair<String, Int>? {
        var highestScore = 0
        var bestItem: String? = null

        for (item in entry.items) {
            val score = scoreItemMatch(item.lowercase(Locale.ROOT), q)
            if (score > highestScore) {
                highestScore = score
                bestItem = item
            }
        }

        return if (highestScore > 0 && bestItem != null) {
            Pair(bestItem, highestScore)
        } else {
            null
        }
    }

    private fun scoreItemMatch(item: String, q: String): Int {
        if (item == q) return 95
        if (item.startsWith(q)) return 88

        val words = item.split(" ", "-", "/")
        if (words.any { it.startsWith(q) }) return 84

        if (q.length >= 2 && item.contains(q)) return 72

        // If query has multiple words, e.g. "whole milk", check if query contains the item word
        if (q.length > item.length && item.length >= 3) {
            if (q.split(" ", "-", "/").any { it == item }) return 86
        }

        return 0
    }

    private fun isMatch(item: String, q: String): Boolean {
        if (item == q) return true
        if (item.startsWith(q)) return true
        if (item.split(" ", "-", "/").any { it.startsWith(q) }) return true
        if (q.length >= 2 && item.contains(q)) return true
        if (q.length > item.length && item.length >= 3 && q.split(" ", "-", "/").any { it == item }) return true
        return false
    }
}

