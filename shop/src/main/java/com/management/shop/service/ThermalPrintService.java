package com.management.shop.service;
import org.springframework.stereotype.Service;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ThermalPrintService {

    // Linux Bluetooth Serial Port (Standard mapping)
    private static final String PRINTER_PORT = "/dev/rfcomm0";

    // ESC/POS Commands (Standard for Niyama/Generic)
    private static final byte[] INIT = {0x1B, 0x40};
    private static final byte[] FEED_LINE = {0x0A};
    private static final byte[] ALIGN_CENTER = {0x1B, 0x61, 0x01};
    private static final byte[] ALIGN_LEFT = {0x1B, 0x61, 0x00};
    private static final byte[] BOLD_ON = {0x1B, 0x45, 0x01};
    private static final byte[] BOLD_OFF = {0x1B, 0x45, 0x00};
    private static final byte[] DOUBLE_HEIGHT = {0x1D, 0x21, 0x11};
    private static final byte[] NORMAL_SIZE = {0x1D, 0x21, 0x00};

    // IMPORTANT: Bluetooth printers sometimes need a tiny delay between commands
    // to avoid buffer overflow, unlike USB.

    public String printOrder(Long orderId) {

        // Use try-with-resources to auto-close the connection
        try (FileOutputStream printer = new FileOutputStream(PRINTER_PORT)) {

            // 1. Initialize
            printer.write(INIT);

            // 2. Header
            printer.write(ALIGN_CENTER);
            printer.write(BOLD_ON);
            printer.write(DOUBLE_HEIGHT);
            printer.write("MY BURGER SHOP\n".getBytes());
            printer.write(NORMAL_SIZE);
            printer.write(BOLD_OFF);
            printer.write(FEED_LINE);

            // 3. Details
            printer.write(ALIGN_LEFT);
            printer.write(("Order #: " + orderId + "\n").getBytes());
            printer.write("--------------------------------\n".getBytes());

            printer.write("1x  Veg Burger       $5.00\n".getBytes());
            printer.write("2x  Coke (L)         $4.00\n".getBytes());

            printer.write("--------------------------------\n".getBytes());

            // 4. Total
            printer.write(BOLD_ON);
            printer.write("TOTAL:               $9.00\n".getBytes());
            printer.write(BOLD_OFF);
            printer.write(FEED_LINE);

            // 5. Footer & Cut
            printer.write(ALIGN_CENTER);
            printer.write("Thank You!\n".getBytes());
            printer.write(FEED_LINE);
            printer.write(FEED_LINE);
            printer.write(FEED_LINE); // Feed to clear the cutter

            // Note: Some small bluetooth printers don't have auto-cutters.
            // If yours doesn't, this command just does nothing.

            printer.flush(); // Force send all data
            return "Bluetooth Print Sent!";

        } catch (IOException e) {
            e.printStackTrace();
            return "Error: Printer disconnected or not bound. Run 'sudo rfcomm bind 0 <MAC>'";
        }
    }
}