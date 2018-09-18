package com.marilone.altijdthuis.packages;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 */
public class DeliveredPackages {

    /**
     * An array of sample (dummy) items.
     */
    public static final List<PackageItem> ITEMS = new ArrayList<>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    private static final Map<String, PackageItem> ITEM_MAP = new HashMap<>();

    public DeliveredPackages(JSONArray mPackages)  {
        try {
            for (int i = 0; i < mPackages.length(); i++) {
                JSONObject oPackage = mPackages.getJSONObject(i);
                int ID = (int) oPackage.get("ID");
                addItem(createPackageItem(oPackage.getString("TEKST"), ID));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private static void addItem(PackageItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }

    private static PackageItem createPackageItem(String mPackage, int i) {
        return new PackageItem(String.valueOf(i),mPackage, makeDetails(i));
    }

    private static String makeDetails(int position) {
        StringBuilder builder = new StringBuilder();
        builder.append("Details about Item: ").append(position);
        for (int i = 0; i < position; i++) {
            builder.append("\nMore details information here.");
        }
        return builder.toString();
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class PackageItem {
        private final String id;
        public final String content;
        private final String details;

        PackageItem(String id, String content, String details) {
            this.id = id;
            this.content = content;
            this.details = details;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return content;
        }
    }
}
