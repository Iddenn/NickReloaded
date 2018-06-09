/*
 * MIT License
 *
 * Copyright (c) 2017 Antoine "Idden" ROCHAS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.idden.nickreloaded.addon;

import io.idden.nickreloaded.NickReloaded;
import io.idden.nickreloaded.addon.papi.PlaceholderAPIAddon;
import io.idden.nickreloaded.addon.result.AddonRegisterResult;
import io.idden.nickreloaded.logger.Logger;

import java.util.HashMap;

/**
 * Manage addon.
 *
 * @author Antoine "Idden" ROCHAS
 * @since 2.0-rc1
 */
public class AddonManager
{
    private HashMap<String, AbstractAddon> ADDONS = new HashMap<>();

    public void loadAddons()
    {
        //PlaceholderAPI
        PlaceholderAPIAddon placeholderAPIAddon = new PlaceholderAPIAddon();

        placeholderAPIAddon.register(new AddonRegisterResult()
        {
            @Override
            public void onSuccess()
            {
                //Registering into map
                ADDONS.putIfAbsent(placeholderAPIAddon.id, placeholderAPIAddon);
            }

            @Override
            public void notFound()
            {
                //Not a requiered dependency
            }

            @Override
            public void onFail()
            {
                //Not a requiered dependency
            }
        });


        NickReloaded.INSTANCE.manager.logger.log(Logger.Level.LOG, "Dependencies loaded !");
    }

    public void unloadAddons()
    {
        ADDONS.forEach((id, addon) ->
        {
            addon.unregister();
            ADDONS.remove(id);
        });
    }
}
