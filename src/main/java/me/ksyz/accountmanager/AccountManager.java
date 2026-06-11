package me.ksyz.accountmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.AccountType;
import me.ksyz.accountmanager.auth.CookieAuth;
import me.ksyz.accountmanager.gui.GuiCookieAuth;
import me.ksyz.accountmanager.utils.SSLUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Optional;

@Mod(modid = "accountmanager", version = "@VERSION@", clientSideOnly = true, acceptedMinecraftVersions = "1.8.9")
public class AccountManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final File file = new File(mc.mcDataDir, "accounts.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final ArrayList<Account> accounts = new ArrayList<Account>();

    @Mod.EventHandler
    public static void init(FMLInitializationEvent event) {
        SSLUtil.getSSLContext();
        MinecraftForge.EVENT_BUS.register(new Events());
        if (!file.exists()) {
            try {
                if ((file.getParentFile().exists() || file.getParentFile().mkdirs()) && file.createNewFile()) {
                    System.out.print("Successfully created accounts.json!");
                }
            } catch (IOException e) {
                System.err.print("Couldn't create accounts.json!");
            }
        }
    }

    public static void load() {
        accounts.clear();
        try {
            JsonElement json = new JsonParser().parse(new BufferedReader(new FileReader(file)));
            if (json instanceof JsonArray) {
                JsonArray jsonArray = json.getAsJsonArray();
                for (JsonElement jsonElement : jsonArray) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    accounts.add(Account.fromJson(jsonObject));
                }
            }
        } catch (FileNotFoundException e) {
            System.err.print("Couldn't find accounts.json!");
        } catch (JsonSyntaxException e) {
            System.err.println("Error parsing accounts.json: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            JsonArray jsonArray = new JsonArray();
            for (Account account : accounts) {
                jsonArray.add(account.toJson());
            }
            PrintWriter printWriter = new PrintWriter(new FileWriter(file));
            printWriter.println(gson.toJson(jsonArray));
            printWriter.close();
        } catch (IOException e) {
            System.err.print("Couldn't save accounts.json!");
        }
    }

    public static void addCrackedAccount(String username) {
        Optional<Account> existingAccount = accounts.stream()
                .filter(acc -> acc.getUsername().equalsIgnoreCase(username) && acc.getType() == AccountType.CRACKED)
                .findFirst();
        if (existingAccount.isPresent()) {
            System.out.println("Cracked account " + username + " already exists. Skipping add.");
            return;
        }
        accounts.add(new Account("", "accessToken", username, "", 0L, AccountType.CRACKED));
        save();
        System.out.println("Cracked account " + username + " added successfully!");
    }

    public static void addAccountFromCookieFile(File cookieFile, GuiScreen previousScreen) {
        GuiCookieAuth gui = new GuiCookieAuth(previousScreen);
        CookieAuth.addAccountFromCookieFile(cookieFile, gui);
    }
}
