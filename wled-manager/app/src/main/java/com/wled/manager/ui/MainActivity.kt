package com.wled.manager.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.wled.manager.R
import com.wled.manager.api.WledApi
import com.wled.manager.databinding.ActivityMainBinding
import com.wled.manager.model.WledDevice
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: DeviceAdapter
    private val devices = mutableListOf<WledDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = DeviceAdapter(devices) { device ->
            val intent = Intent(this, ControlActivity::class.java)
            intent.putExtra(ControlActivity.EXTRA_IP, device.ip)
            intent.putExtra(ControlActivity.EXTRA_NAME, device.name)
            startActivity(intent)
        }

        binding.recyclerDevices.layoutManager = LinearLayoutManager(this)
        binding.recyclerDevices.adapter = adapter

        binding.fabAdd.setOnClickListener { showAddDeviceDialog() }

        loadSavedDevices()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> { refreshAll(); true }
            R.id.action_scan -> { showScanDialog(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAddDeviceDialog() {
        val input = TextInputEditText(this).apply {
            hint = getString(R.string.hint_ip_address)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.add_device)
            .setView(input)
            .setPositiveButton(R.string.add) { _, _ ->
                val ip = input.text?.toString()?.trim() ?: return@setPositiveButton
                if (ip.isNotEmpty()) addDevice(ip)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showScanDialog() {
        val prefs = getSharedPreferences("wled_prefs", MODE_PRIVATE)
        val subnet = prefs.getString("last_subnet", "192.168.1") ?: "192.168.1"
        val input = TextInputEditText(this).apply {
            setText(subnet)
            hint = "192.168.1"
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.scan_network)
            .setMessage(R.string.scan_message)
            .setView(input)
            .setPositiveButton(R.string.scan) { _, _ ->
                val sub = input.text?.toString()?.trim() ?: return@setPositiveButton
                if (sub.isNotEmpty()) {
                    prefs.edit().putString("last_subnet", sub).apply()
                    scanNetwork(sub)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun addDevice(ip: String) {
        if (devices.any { it.ip == ip }) {
            Snackbar.make(binding.root, R.string.device_already_added, Snackbar.LENGTH_SHORT).show()
            return
        }
        val device = WledDevice(ip)
        devices.add(device)
        adapter.notifyItemInserted(devices.size - 1)
        saveDevices()
        checkDevice(device, devices.size - 1)
    }

    private fun checkDevice(device: WledDevice, position: Int) {
        lifecycleScope.launch {
            val api = WledApi(device.ip)
            val online = api.ping()
            if (online) {
                val info = api.getInfo().getOrNull()
                if (info != null && info.name.isNotEmpty()) {
                    device.name = info.name
                }
            }
            device.isOnline = online
            adapter.notifyItemChanged(position)
        }
    }

    private fun refreshAll() {
        devices.forEachIndexed { i, device -> checkDevice(device, i) }
        Snackbar.make(binding.root, R.string.refreshing, Snackbar.LENGTH_SHORT).show()
    }

    private fun scanNetwork(subnet: String) {
        binding.fabAdd.isEnabled = false
        Snackbar.make(binding.root, R.string.scanning, Snackbar.LENGTH_LONG).show()
        lifecycleScope.launch {
            val found = mutableListOf<WledDevice>()
            val jobs = (1..254).map { last ->
                kotlinx.coroutines.async(kotlinx.coroutines.Dispatchers.IO) {
                    val ip = "$subnet.$last"
                    val api = WledApi(ip)
                    if (api.ping()) {
                        val info = api.getInfo().getOrNull()
                        WledDevice(ip, info?.name?.takeIf { it.isNotEmpty() } ?: ip, true)
                    } else null
                }
            }
            jobs.forEach { job ->
                val device = job.await()
                if (device != null && devices.none { it.ip == device.ip }) {
                    found.add(device)
                }
            }
            if (found.isEmpty()) {
                Snackbar.make(binding.root, R.string.no_devices_found, Snackbar.LENGTH_LONG).show()
            } else {
                val start = devices.size
                devices.addAll(found)
                adapter.notifyItemRangeInserted(start, found.size)
                saveDevices()
                Snackbar.make(binding.root, getString(R.string.found_devices, found.size), Snackbar.LENGTH_LONG).show()
            }
            binding.fabAdd.isEnabled = true
        }
    }

    private fun saveDevices() {
        val prefs = getSharedPreferences("wled_prefs", MODE_PRIVATE)
        val ips = devices.joinToString(",") { it.ip }
        prefs.edit().putString("devices", ips).apply()
    }

    private fun loadSavedDevices() {
        val prefs = getSharedPreferences("wled_prefs", MODE_PRIVATE)
        val saved = prefs.getString("devices", "") ?: ""
        if (saved.isNotEmpty()) {
            saved.split(",").filter { it.isNotEmpty() }.forEach { ip ->
                val device = WledDevice(ip)
                devices.add(device)
                checkDevice(device, devices.size - 1)
            }
            adapter.notifyDataSetChanged()
        }
    }
}
