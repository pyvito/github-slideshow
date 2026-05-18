package com.wled.manager.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wled.manager.R
import com.wled.manager.databinding.ItemDeviceBinding
import com.wled.manager.model.WledDevice

class DeviceAdapter(
    private val devices: List<WledDevice>,
    private val onClick: (WledDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(device: WledDevice) {
            binding.textDeviceName.text = device.name
            binding.textDeviceIp.text = device.ip
            binding.imageStatus.setImageResource(
                if (device.isOnline) R.drawable.ic_online else R.drawable.ic_offline
            )
            binding.root.setOnClickListener { onClick(device) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(devices[position])

    override fun getItemCount() = devices.size
}
