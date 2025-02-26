import java.util.*;
import java.io.*;

class Stock {
    private String dateEntered;
    private String label;
    private String brand;
    private String engineNumber;
    private String status;

    public Stock(String dateEntered, String label, String brand,
                 String engineNumber, String status) {
        this.dateEntered = dateEntered;
        this.label = label;
        this.brand = brand;
        this.engineNumber = engineNumber;
        this.status = status;
    }

    // Getters
    public String getDateEntered() { return dateEntered; }
    public String getLabel() { return label; }
    public String getBrand() { return brand; }
    public String getEngineNumber() { return engineNumber; }
    public String getStatus() { return status; }

    public boolean matches(String criteria) {
        return dateEntered.equalsIgnoreCase(criteria) ||
                label.equalsIgnoreCase(criteria) ||
                brand.equalsIgnoreCase(criteria) ||
                engineNumber.equalsIgnoreCase(criteria) ||
                status.equalsIgnoreCase(criteria);
    }

    @Override
    public String toString() {
        return String.format("Date: %-10s | Label: %-3s | Brand: %-10s | Engine: %-15s | Status: %-6s",
                dateEntered, label, brand, engineNumber, status);
    }
}

public class Main {
    private static final String CSV_FILE = "src/motorph_invdata.csv";
    private static final Scanner scanner = new Scanner(System.in);

    private static Map<String, Stock> stockHashTable = new HashMap<>();  // Fast engine# lookup
    private static List<Stock> stockList = new ArrayList<>();            // Maintain order

    public static void main(String[] args) {

        loadInventory();

        while (true) {
            System.out.println("\n=== MotorPH Inventory System ===");
            System.out.println("1. Add New Stock");
            System.out.println("2. Delete Stock");
            System.out.println("3. Sort by Brand");
            System.out.println("4. Search Inventory");
            System.out.println("5. Exit");
            System.out.print("Select option: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine());
                switch (choice) {
                    case 1: addStock(); break;
                    case 2: deleteStock(); break;
                    case 3: sortStocks(); break;
                    case 4: searchStocks(); break;
                    case 5: exit(); return;
                    default: System.out.println("Invalid option!");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number!");
            }
        }
    }

    private static void loadInventory() {
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length == 5) {
                    Stock stock = new Stock(
                            data[0].trim(),
                            data[1].trim().toLowerCase(),
                            data[2].trim(),
                            data[3].trim(),
                            data[4].trim().toLowerCase()
                    );
                    stockList.add(stock);
                    stockHashTable.put(stock.getEngineNumber().toLowerCase(), stock);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    private static void addStock() {
        List<Stock> stocks = new ArrayList<>(readCSV());

        System.out.println("\n--- Add New Stock ---");
        String date = getStringInput("Enter date: ");
        String label = getValidInput("Label (old/new): ", Arrays.asList("old", "new"));
        String brand = getStringInput("Enter brand: ");
        String engine = getUniqueEngineNumber(stocks);
        String status = getValidInput("Status (on-hand/sold): ", Arrays.asList("on-hand", "sold"));

        Stock newStock = new Stock(date, label, brand, engine, status);

        stockList.add(newStock);
        stockHashTable.put(engine.toLowerCase(), newStock);

        stocks.add(new Stock(date, label, brand, engine, status));

        writeCSV();
        System.out.println("Stock added successfully!");
    }

    private static void deleteStock() {
        List<Stock> stocks = new ArrayList<>(readCSV());
        if(stocks.isEmpty()) {
            System.out.println("Inventory is empty!");
            return;
        }

        String engine = getStringInput("\nEnter engine number to delete: ").toLowerCase();

        // Check hash table for instant existence check
        if (!stockHashTable.containsKey(engine)) {
            System.out.println("Error: Engine number not found!");
            return;
        }

        Stock removed = stockHashTable.remove(engine);
        boolean listRemoved = stockList.removeIf(s ->
                s.getEngineNumber().equalsIgnoreCase(engine));

        if (listRemoved) {
            writeCSV();  // Persist changes to file
            System.out.println("Stock deleted successfully!");
        } else {
            // Data inconsistency fallback
            stockHashTable.put(engine, removed);  // Restore hash table entry
            System.out.println("Error: Failed to remove from inventory list!");
        }
    }


    // ================== IMPROVED USING MERGE SORT & RECURSION ==================
    private static void sortStocks() {
        stockList = mergeSort(stockList);  // Recursive merge sort
        writeCSV();
        System.out.println("\nStocks sorted by brand!");
        displayStocks(stockList);
    }

    private static List<Stock> mergeSort(List<Stock> list) {
        if (list.size() <= 1) return list;

        // Recursive division
        int mid = list.size() / 2;
        List<Stock> left = mergeSort(list.subList(0, mid));
        List<Stock> right = mergeSort(list.subList(mid, list.size()));

        // Merge phase
        return merge(left, right);
    }

    private static List<Stock> merge(List<Stock> left, List<Stock> right) {
        List<Stock> merged = new ArrayList<>();
        int i = 0, j = 0;

        while (i < left.size() && j < right.size()) {
            if (left.get(i).getBrand().compareTo(right.get(j).getBrand()) < 0) {
                merged.add(left.get(i++));
            } else {
                merged.add(right.get(j++));
            }
        }

        // Add remaining elements
        merged.addAll(left.subList(i, left.size()));
        merged.addAll(right.subList(j, right.size()));
        return merged;
    }

    // ================== IMPROVED USING HASHING SEARCH ==================
    private static void searchStocks() {
        String criteria = getStringInput("\nEnter search term: ");

        // First check hash table for exact engine no. match
        Stock exactMatch = stockHashTable.get(criteria.toLowerCase());
        if (exactMatch != null) {
            System.out.println("\nExact match found:");
            System.out.println(exactMatch);
            return;
        }

        // Linear search for partial matches
        List<Stock> results = new ArrayList<>();
        for (Stock stock : stockList) {
            if (stock.matches(criteria)) results.add(stock);
        }

        if(results.isEmpty()) {
            System.out.println("No matching stocks found!");
        } else {
            System.out.println("\nSearch Results (" + results.size() + " found):");
            displayStocks(results);
        }
    }

    private static List<Stock> readCSV() {
        List<Stock> stocks = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
            br.readLine(); // Skip header
            String line;
            while((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if(data.length == 5) {
                    stocks.add(new Stock(
                            data[0].trim(),
                            data[1].trim().toLowerCase(),
                            data[2].trim(),
                            data[3].trim(),
                            data[4].trim().toLowerCase()
                    ));
                } else {
                    System.out.println("Skipping malformed record: " + line);
                }
            }
        } catch(IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        return stocks;
    }

    // ================== MODIFIED WRITE METHOD ==================
    private static void writeCSV() {
        try (FileWriter writer = new FileWriter(CSV_FILE)) {
            writer.write("Date Entered,Label,Brand,Engine Number,Status\n");
            for (Stock s : stockList) {
                writer.write(String.format("%s,%s,%s,%s,%s\n",
                        s.getDateEntered(),
                        s.getLabel(),
                        s.getBrand(),
                        s.getEngineNumber(),
                        s.getStatus()));
            }
            writer.flush();
        } catch (IOException e) {
            System.out.println("Error writing file: " + e.getMessage());
        }
    }

    // Helper methods
    private static String getUniqueEngineNumber(List<Stock> stocks) {
        while(true) {
            String engine = getStringInput("Enter engine number: ").trim();
            if(engine.isEmpty()) {
                System.out.println("Engine number cannot be empty!");
                continue;
            }
            boolean exists = stocks.stream()
                    .anyMatch(s -> s.getEngineNumber().equalsIgnoreCase(engine));
            if(!exists) return engine;
            System.out.println("Engine number already exists! Please enter a new one.");
        }
    }

    private static String getValidInput(String prompt, List<String> validOptions) {
        while(true) {
            String input = getStringInput(prompt).toLowerCase();
            if(validOptions.contains(input)) return input;
            System.out.println("Invalid input! Valid options: " + validOptions);
        }
    }

    private static String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static void displayStocks(List<Stock> stocks) {
        System.out.println("\nCurrent Inventory:");
        System.out.println("--------------------------------------------------------------------------------");
        stocks.forEach(System.out::println);
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("Total items: " + stocks.size());
    }

    private static void exit() {
        System.out.println("\nSaving data...");
        System.out.println("Thank you for using MotorPH Inventory System!");
        scanner.close();
    }
}