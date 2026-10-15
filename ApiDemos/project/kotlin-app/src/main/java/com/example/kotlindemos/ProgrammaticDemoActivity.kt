// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.example.kotlindemos

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.common_ui.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.addMarker
import com.google.maps.android.awaitMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Reproduction Demo: Demonstrates the failure to provide a Maps API key dynamically at runtime.
 *
 * In multi-tenant enterprise environments, API keys are fetched after authentication.
 * Because there is no public API to inject the retrieved key into MapsInitializer or
 * SupportMapFragment, GMS Core falls back to manifest metadata (empty), caching an auth failure.
 */
class ProgrammaticDemoActivity : SamplesBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_base)

        Toast.makeText(this, "Simulating backend auth & key retrieval...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            // 1. Simulate network latency when retrieving credentials from an authentication/config server
            delay(1200)

            // 2. Simulated key received dynamically at runtime
            val dynamicApiKey = "AIzaSyDummyKeyForReproTesting1234567890"
            Log.i(TAG, "Dynamic API key retrieved from remote service: $dynamicApiKey")

            /*
             * =========================================================================
             * MISSING API / SDK LIMITATION:
             * =========================================================================
             * There is currently no public method in the Maps SDK to supply this key:
             *
             *   MapsInitializer.initialize(context, apiKey, renderer, callback)  // MISSING
             *   mapFragment.setApiKey(dynamicApiKey)                             // MISSING
             *
             * Because no programmatic entry point exists, GMS Core queries the empty
             * AndroidManifest metadata via IPC, failing authorization permanently:
             *   E/Google Android Maps SDK: Authorization failure: API Key:
             * =========================================================================
             */
            MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST) { renderer ->
                Log.d(TAG, "Maps initialized with renderer: $renderer")
            }

            // 3. Programmatically attach the SupportMapFragment
            var mapFragment = supportFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) as? SupportMapFragment
            if (mapFragment == null) {
                mapFragment = SupportMapFragment.newInstance()
                supportFragmentManager.beginTransaction()
                    .add(R.id.sample_content_container, mapFragment, MAP_FRAGMENT_TAG)
                    .commit()
            }

            // 4. Await map and verify failure to load tiles
            val map = mapFragment.awaitMap()
            Log.i(TAG, "awaitMap completed. Tiles fail to render due to empty manifest key.")

            val sydney = LatLng(-33.852, 151.211)
            map.addMarker {
                position(sydney)
                title("Sydney Opera House")
            }
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 10f))
        }
    }

    companion object {
        private const val TAG = "DynamicKeyRepro"
        private const val MAP_FRAGMENT_TAG = "map"
    }
}
