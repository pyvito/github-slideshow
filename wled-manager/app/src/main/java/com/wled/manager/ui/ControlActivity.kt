package com.wled.manager.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.wled.manager.R
import com.wled.manager.api.WledApi
import com.wled.manager.databinding.ActivityControlBinding
import com.wled.manager.model.SegmentCommand
import com.wled.manager.model.StateCommand
import com.wled.manager.model.WledState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ControlActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IP = "extra_ip"
        const val EXTRA_NAME = "extra_name"
    }

    private lateinit var binding: ActivityControlBinding
    private lateinit var api: WledApi

    private var currentState: WledState? = null
    private var effects: List<String> = emptyList()
    private var palettes: List<String> = emptyList()

    private var debounceJob: Job? = null
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ip = intent.getStringExtra(EXTRA_IP) ?: run { finish(); return }
        val name = intent.getStringExtra(EXTRA_NAME) ?: ip

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = name
        supportActionBar?.subtitle = ip

        api = WledApi(ip)

        setupListeners()
        loadData()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupListeners() {
        binding.switchPower.setOnCheckedChangeListener { _, isChecked ->
            if (!isLoading) sendState(StateCommand(on = isChecked))
        }

        binding.seekBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.textBrightness.text = getString(R.string.brightness_value, progress)
                    debounce { sendState(StateCommand(brightness = progress)) }
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        binding.seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) debounce { sendSegment(SegmentCommand(speed = progress)) }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        binding.seekIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) debounce { sendSegment(SegmentCommand(intensity = progress)) }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        binding.spinnerEffect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (!isLoading) sendSegment(SegmentCommand(effect = pos))
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.spinnerPalette.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (!isLoading) sendSegment(SegmentCommand(palette = pos))
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Color buttons
        val colorButtons = listOf(
            binding.btnColorRed to listOf(255, 0, 0),
            binding.btnColorOrange to listOf(255, 128, 0),
            binding.btnColorYellow to listOf(255, 255, 0),
            binding.btnColorGreen to listOf(0, 255, 0),
            binding.btnColorCyan to listOf(0, 255, 255),
            binding.btnColorBlue to listOf(0, 0, 255),
            binding.btnColorPurple to listOf(128, 0, 255),
            binding.btnColorPink to listOf(255, 0, 128),
            binding.btnColorWhite to listOf(255, 255, 255),
            binding.btnColorWarm to listOf(255, 160, 60)
        )
        colorButtons.forEach { (btn, rgb) ->
            btn.setOnClickListener {
                sendSegment(SegmentCommand(colors = listOf(rgb)))
                updateColorPreview(rgb[0], rgb[1], rgb[2])
            }
        }

        binding.btnRefresh.setOnClickListener { loadState() }
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val effectsResult = api.getEffects()
            val palettesResult = api.getPalettes()

            effects = effectsResult.getOrDefault(listOf("Solid"))
            palettes = palettesResult.getOrDefault(listOf("Default"))

            binding.spinnerEffect.adapter = ArrayAdapter(
                this@ControlActivity,
                android.R.layout.simple_spinner_item,
                effects
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

            binding.spinnerPalette.adapter = ArrayAdapter(
                this@ControlActivity,
                android.R.layout.simple_spinner_item,
                palettes
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

            loadState()
        }
    }

    private fun loadState() {
        lifecycleScope.launch {
            isLoading = true
            val result = api.getState()
            result.onSuccess { state ->
                currentState = state
                applyStateToUi(state)
            }.onFailure {
                Snackbar.make(binding.root, R.string.connection_error, Snackbar.LENGTH_SHORT).show()
            }
            binding.progressBar.visibility = View.GONE
            isLoading = false
        }
    }

    private fun applyStateToUi(state: WledState) {
        binding.switchPower.isChecked = state.on
        binding.seekBrightness.progress = state.brightness
        binding.textBrightness.text = getString(R.string.brightness_value, state.brightness)

        val seg = state.segments.firstOrNull()
        if (seg != null) {
            binding.seekSpeed.progress = seg.speed
            binding.seekIntensity.progress = seg.intensity
            if (effects.isNotEmpty() && seg.effect < effects.size)
                binding.spinnerEffect.setSelection(seg.effect)
            if (palettes.isNotEmpty() && seg.palette < palettes.size)
                binding.spinnerPalette.setSelection(seg.palette)
            val col = seg.colors.firstOrNull()
            if (col != null && col.size >= 3)
                updateColorPreview(col[0], col[1], col[2])
        }
    }

    private fun updateColorPreview(r: Int, g: Int, b: Int) {
        binding.viewColorPreview.setBackgroundColor(Color.rgb(r, g, b))
    }

    private fun sendState(command: StateCommand) {
        lifecycleScope.launch {
            api.setState(command).onFailure {
                Snackbar.make(binding.root, R.string.send_error, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendSegment(segCmd: SegmentCommand) {
        sendState(StateCommand(segments = listOf(segCmd)))
    }

    private fun debounce(action: suspend () -> Unit) {
        debounceJob?.cancel()
        debounceJob = lifecycleScope.launch {
            delay(300)
            action()
        }
    }
}
