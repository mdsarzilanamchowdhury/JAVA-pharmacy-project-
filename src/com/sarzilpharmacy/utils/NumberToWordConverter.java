package com.sarzilpharmacy.utils;

public class NumberToWordConverter {
    private static final String[] units = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", 
        "Eighteen", "Nineteen"
    };
    
    private static final String[] tens = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };
    
    public static String convert(int n) {
        if (n == 0) {
            return "Zero";
        }
        
        if (n < 0) {
            return "Minus " + convert(-n);
        }
        
        String words = "";
        
        // Handle lakhs
        if ((n / 100000) > 0) {
            words += convert(n / 100000) + " Lac ";
            n %= 100000;
        }
        
        // Handle thousands
        if ((n / 1000) > 0) {
            words += convert(n / 1000) + " Thousand ";
            n %= 1000;
        }
        
        // Handle hundreds
        if ((n / 100) > 0) {
            words += convert(n / 100) + " Hundred ";
            n %= 100;
        }
        
        // Handle units and tens
        if (n > 0) {
            if (n < 20) {
                words += units[n];
            } else {
                words += tens[n / 10];
                if ((n % 10) > 0) {
                    words += " " + units[n % 10];
                }
            }
        }
        
        return words.trim() + " Taka Only";
    }
    
    // Test method
    public static void main(String[] args) {
        System.out.println(convert(220));    // Two Hundred Twenty Taka Only
        System.out.println(convert(1575));   // One Thousand Five Hundred Seventy Five Taka Only
        System.out.println(convert(100000)); // One Lac Taka Only
    }
}
