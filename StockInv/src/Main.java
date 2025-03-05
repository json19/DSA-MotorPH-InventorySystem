import java.util.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

class Stock {
    private String rawDate;
    private LocalDate parsedDate;
    private String label;
    private String brand;
    private String engineNumber;
    private String status;

    public Stock(String date, String label, String brand, String engine, String status) {
        this.rawDate = date.trim();
        this.label = label.toLowerCase();
        this.brand = brand.trim();
        this.engineNumber = engine.trim();
        this.status = status.toLowerCase();
        this.parsedDate = parseDate(this.rawDate);
    }

    private LocalDate parseDate(String dateString) {
        List<DateTimeFormatter> formats = Arrays.asList(
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        );

        for(DateTimeFormatter format : formats) {
            try {
                return LocalDate.parse(dateString, format);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        System.out.println("Invalid date '" + dateString + "', using current date");
        return LocalDate.now();
    }

    // Getters
    public String getBrand() { return brand; }
    public String getEngineNumber() { return engineNumber; }
    public String getLabel() { return label; }
    public String getStatus() { return status; }
    public String getRawDate() { return rawDate; }
    public LocalDate getParsedDate() { return parsedDate; }

    public boolean matches(String criteria) {
        return rawDate.equalsIgnoreCase(criteria) ||
                label.equalsIgnoreCase(criteria) ||
                brand.equalsIgnoreCase(criteria) ||
                engineNumber.equalsIgnoreCase(criteria) ||
                status.equalsIgnoreCase(criteria);
    }

    @Override
    public String toString() {
        return String.format("Date: %-10s | Label: %-3s | Brand: %-10s | Engine: %-15s | Status: %-6s",
                rawDate, label, brand, engineNumber, status);
    }
}

class BSTNode {
    String brand;
    List<Stock> stocks = new ArrayList<>();
    BSTNode left, right;

    BSTNode(String brand, Stock stock) {
        this.brand = brand;
        this.stocks.add(stock);
    }
}

class BrandBST {
    BSTNode root;

    void insert(Stock stock) {
        root = insertRec(root, stock);
    }

    private BSTNode insertRec(BSTNode node, Stock stock) {
        if(node == null) return new BSTNode(stock.getBrand(), stock);

        int cmp = stock.getBrand().compareToIgnoreCase(node.brand);
        if(cmp < 0) node.left = insertRec(node.left, stock);
        else if(cmp > 0) node.right = insertRec(node.right, stock);
        else node.stocks.add(stock);

        return node;
    }

    void remove(String brand, String engine) {
        root = removeRec(root, brand, engine);
    }

    private BSTNode removeRec(BSTNode node, String brand, String engine) {
        if(node == null) return null;

        int cmp = brand.compareToIgnoreCase(node.brand);
        if(cmp < 0) node.left = removeRec(node.left, brand, engine);
        else if(cmp > 0) node.right = removeRec(node.right, brand, engine);
        else node.stocks.removeIf(s -> s.getEngineNumber().equalsIgnoreCase(engine));

        if(node.stocks.isEmpty()) {
            if(node.left == null) return node.right;
            if(node.right == null) return node.left;

            BSTNode temp = minValueNode(node.right);
            node.brand = temp.brand;
            node.stocks = temp.stocks;
            node.right = removeRec(node.right, temp.brand, temp.stocks.get(0).getEngineNumber());
        }

        return node;
    }

    private BSTNode minValueNode(BSTNode node) {
        BSTNode current = node;
        while(current.left != null) current = current.left;
        return current;
    }

    List<Stock> getSortedStocks() {
        List<Stock> result = new ArrayList<>();
        inOrderTraversal(root, result);
        return result;
    }

    private void inOrderTraversal(BSTNode node, List<Stock> result) {
        if(node != null) {
            inOrderTraversal(node.left, result);
            result.addAll(node.stocks);
            inOrderTraversal(node.right, result);
        }
    }
}

public class Main {
    private static final String CSV_FILE = "src/motorph_invdata.csv";
    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter DEFAULT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private static List<Stock> stockList = new ArrayList<>();
    private static Map<String, Stock> stockHashTable = new HashMap<>();
    private static BrandBST brandBST = new BrandBST();

    public static void main(String[] args) {
        loadInventory();
        while(true) {
            System.out.println("\n=== MotorPH Inventory System ===");
            System.out.println("1. Add New Stock");
            System.out.println("2. Delete Stock");
            System.out.println("3. Sort by Brand");
            System.out.println("4. Search Inventory");
            System.out.println("5. Exit");
            System.out.print("Select option: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine());
                switch(choice) {
                    case 1: addStock(); break;
                    case 2: deleteStock(); break;
                    case 3: sortStocks(); break;
                    case 4: searchStocks(); break;
                    case 5: exit(); return;
                    default: System.out.println("Invalid option!");
                }
            } catch(NumberFormatException e) {
                System.out.println("Please enter a valid number!");
            }
        }
    }

    private static void loadInventory() {
        stockList.clear();
        stockHashTable.clear();
        brandBST = new BrandBST();

        try(BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
            br.readLine(); // Skip header
            String line;
            while((line = br.readLine()) != null) {
                String[] data = line.split(",", 5);
                if(data.length == 5) {
                    Stock stock = new Stock(
                            data[0], data[1], data[2], data[3], data[4]
                    );
                    stockList.add(stock);
                    stockHashTable.put(stock.getEngineNumber().toLowerCase(), stock);
                    brandBST.insert(stock);
                }
            }
        } catch(IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    private static void addStock() {
        System.out.println("\n--- Add New Stock ---");

        String date = getDateInput();
        String label = getValidInput("Label (old/new) [default: new]: ",
                Arrays.asList("old", "new"), "new");
        String brand = getStringInput("Enter brand: ");
        String engine = getUniqueEngineNumber();
        String status = getValidInput("Status (on-hand/sold) [default: on-hand]: ",
                Arrays.asList("on-hand", "sold"), "on-hand");

        Stock newStock = new Stock(date, label, brand, engine, status);

        stockList.add(newStock);
        stockHashTable.put(engine.toLowerCase(), newStock);
        brandBST.insert(newStock);

        writeCSV();
        System.out.println("Stock added successfully!");
    }

    private static void deleteStock() {
        if(stockList.isEmpty()) {
            System.out.println("Inventory is empty!");
            return;
        }

        String engine = getStringInput("\nEnter engine number to delete: ").toLowerCase();
        Stock stock = stockHashTable.get(engine);

        if(stock == null) {
            System.out.println("Error: Engine number not found!");
            return;
        }

        if(stock.getLabel().equals("old") && stock.getStatus().equals("sold")) {
            String confirm = getValidInput(
                    "WARNING: This is an old sold stock. Confirm deletion? (y/n): ",
                    Arrays.asList("y", "n"), "n"
            );
            if(!confirm.equalsIgnoreCase("y")) return;
        }

        stockList.removeIf(s -> s.getEngineNumber().equalsIgnoreCase(engine));
        stockHashTable.remove(engine);
        brandBST.remove(stock.getBrand(), engine);

        writeCSV();
        System.out.println("Stock deleted successfully!");
    }

    private static void sortStocks() {
        stockList = brandBST.getSortedStocks();
        writeCSV();
        System.out.println("\nStocks sorted by brand:");
        displayStocks(stockList);
    }

    private static void searchStocks() {
        String criteria = getStringInput("\nEnter search term: ").toLowerCase();

        // Check for exact engine match first
        Stock exactMatch = stockHashTable.get(criteria);
        if(exactMatch != null) {
            System.out.println("\nExact match found:");
            System.out.println(exactMatch);
            return;
        }

        List<Stock> results = new ArrayList<>();
        for(Stock stock : stockList) {
            if(stock.matches(criteria)) results.add(stock);
        }

        if(results.isEmpty()) {
            System.out.println("No matching stocks found!");
        } else {
            System.out.println("\nSearch Results (" + results.size() + " found):");
            displayStocks(results);
        }
    }

    private static String getDateInput() {
        while(true) {
            String input = getStringInput("Enter date (MM/DD/YYYY) [default: today]: ");
            if(input.isEmpty()) {
                return LocalDate.now().format(DEFAULT_DATE_FORMAT);
            }

            try {
                LocalDate.parse(input, DEFAULT_DATE_FORMAT);
                return input;
            } catch(DateTimeParseException e) {
                System.out.println("Invalid date format! Use MM/DD/YYYY");
            }
        }
    }

    private static String getUniqueEngineNumber() {
        while(true) {
            String engine = getStringInput("Enter engine number: ").trim();
            if(engine.isEmpty()) {
                System.out.println("Engine number cannot be empty!");
                continue;
            }
            if(!stockHashTable.containsKey(engine.toLowerCase())) {
                return engine;
            }
            System.out.println("Engine number already exists! Please enter a new one.");
        }
    }

    private static String getValidInput(String prompt, List<String> options, String defaultValue) {
        while(true) {
            String input = getStringInput(prompt).toLowerCase();
            if(input.isEmpty()) return defaultValue;
            if(options.contains(input)) return input;
            System.out.println("Invalid input! Valid options: " + options);
        }
    }

    private static String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static void writeCSV() {
        try(FileWriter writer = new FileWriter(CSV_FILE)) {
            writer.write("Date Entered,Label,Brand,Engine Number,Status\n");
            for(Stock s : stockList) {
                writer.write(String.format("%s,%s,%s,%s,%s%n",
                        s.getRawDate(),
                        s.getLabel(),
                        s.getBrand(),
                        s.getEngineNumber(),
                        s.getStatus()));
            }
        } catch(IOException e) {
            System.out.println("Error writing file: " + e.getMessage());
        }
    }

    private static void displayStocks(List<Stock> stocks) {
        System.out.println("--------------------------------------------------------------------------------");
        stocks.forEach(System.out::println);
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("Total items: " + stocks.size());
    }

    private static void exit() {
        System.out.println("\nSaving data...");
        writeCSV();
        System.out.println("Thank you for using MotorPH Inventory System!");
        scanner.close();
    }
}