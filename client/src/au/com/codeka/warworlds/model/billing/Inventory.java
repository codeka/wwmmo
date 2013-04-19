/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package au.com.codeka.warworlds.model.billing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a block of information about in-app items.
 * An Inventory is returned by such methods as {@link IabHelper#queryInventory}.
 */
public class Inventory {
    Map<String, SkuDetails> mSkuMap = new HashMap<String, SkuDetails>();
    Map<String, Purchase> mPurchaseMap = new HashMap<String, Purchase>();

    Inventory() {
    }

    /** Returns the listing details for an in-app product. */
    public SkuDetails getSkuDetails(String sku) {
        return mSkuMap.get(sku);
    }

    /** Returns purchase information for a given product, or null if there is no purchase. */
    public Purchase getPurchase(String sku) {
        return mPurchaseMap.get(sku);
    }

    /** Returns whether or not there exists a purchase of the given product. */
    public boolean hasPurchase(String sku) {
        return mPurchaseMap.containsKey(sku);
    }

    /** Return whether or not details about the given product are available. */
    public boolean hasDetails(String sku) {
        return mSkuMap.containsKey(sku);
    }

    /**
     * Erase a purchase (locally) from the inventory, given its product ID. This just
     * modifies the Inventory object locally and has no effect on the server! This is
     * useful when you have an existing Inventory object which you know to be up to date,
     * and you have just consumed an item successfully, which means that erasing its
     * purchase data from the Inventory you already have is quicker than querying for
     * a new Inventory.
     */
    public void erasePurchase(String sku) {
        if (mPurchaseMap.containsKey(sku)) mPurchaseMap.remove(sku);
    }

    /** Returns a list of all owned product IDs. */
    List<String> getAllOwnedSkus() {
        return new ArrayList<String>(mPurchaseMap.keySet());
    }

    /** Returns a list of all purchases. */
    List<Purchase> getAllPurchases() {
        return new ArrayList<Purchase>(mPurchaseMap.values());
    }

    void addSkuDetails(SkuDetails d) {
        mSkuMap.put(d.getSku(), d);
    }

    void addPurchase(Purchase p) {
        mPurchaseMap.put(p.getSku(), p);
    }

    public String toJson() throws JSONException {
        JSONObject json = new JSONObject();

        JSONObject skumapJson = new JSONObject();
        for (String key : mSkuMap.keySet()) {
            JSONObject skuJson = new JSONObject(mSkuMap.get(key).toJson());
            skumapJson.put(key, skuJson);
        }
        json.put("skuMap", skumapJson);

        JSONObject purchasesJson = new JSONObject();
        for (String key : mPurchaseMap.keySet()) {
            JSONObject purchaseJson = new JSONObject(mPurchaseMap.get(key).toJson());
            purchasesJson.put(key, purchaseJson);
        }
        json.put("purchases", purchasesJson);

        return json.toString();
    }

    public static Inventory fromJson(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);

        Inventory inv = new Inventory();

        JSONObject skumapJson = json.getJSONObject("skuMap");
        @SuppressWarnings("unchecked")
        Iterator<String> skuKeys = (Iterator<String>) skumapJson.keys();
        while (skuKeys.hasNext()) {
            String key = skuKeys.next();
            inv.mSkuMap.put(key, new SkuDetails(skumapJson.getJSONObject(key).toString()));
        }

        JSONObject purchasesJson = json.getJSONObject("purchases");
        @SuppressWarnings("unchecked")
        Iterator<String> purchaseKeys = (Iterator<String>) purchasesJson.keys();
        while (purchaseKeys.hasNext()) {
            String key = purchaseKeys.next();
            inv.mPurchaseMap.put(key, new Purchase(purchasesJson.getJSONObject(key).toString(), ""));
        }
 
        return inv;
    }
}
