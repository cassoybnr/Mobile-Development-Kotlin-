package com.example.compoundinterest

import android.animation.ValueAnimator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    // ── UI References ──────────────────────────────────────────────────────────
    private lateinit var spinnerCalculate: Spinner
    private lateinit var spinnerCompound: Spinner
    private lateinit var tvFormula: TextView

    private lateinit var layoutPrincipal: LinearLayout
    private lateinit var layoutRate: LinearLayout
    private lateinit var layoutTime: LinearLayout
    private lateinit var layoutTotal: LinearLayout

    private lateinit var etPrincipal: EditText
    private lateinit var etRate: EditText
    private lateinit var etTime: EditText
    private lateinit var etTotal: EditText

    private lateinit var btnCalculate: Button
    private lateinit var btnClear: Button

    private lateinit var cardResult: androidx.cardview.widget.CardView
    private lateinit var tvResultLabel: TextView
    private lateinit var tvResultValue: TextView
    private lateinit var tvPrincipal: TextView
    private lateinit var tvInterest: TextView
    private lateinit var tvSteps: TextView

    private lateinit var lineChart: LineChart

    private lateinit var cardInsight: androidx.cardview.widget.CardView
    private lateinit var tvInsight: TextView
    private lateinit var tvWhatIf: TextView

    // ── Data ───────────────────────────────────────────────────────────────────
    private val calculateOptions = arrayOf(
        "Total P+I (A)",
        "Principal (P) using A",
        "Principal (P) using I",
        "Rate (R)",
        "Time (t)"
    )

    private val compoundOptions = arrayOf(
        "Continuously",
        "Daily (365)",
        "Daily (360)",
        "Weekly",
        "Biweekly",
        "Semimonthly",
        "Monthly",
        "Bimonthly",
        "Quarterly",
        "Semiannually",
        "Annually"
    )

    private val compoundN = mapOf(
        "Continuously" to -1.0,
        "Daily (365)" to 365.0,
        "Daily (360)" to 360.0,
        "Weekly" to 52.0,
        "Biweekly" to 26.0,
        "Semimonthly" to 24.0,
        "Monthly" to 12.0,
        "Bimonthly" to 6.0,
        "Quarterly" to 4.0,
        "Semiannually" to 2.0,
        "Annually" to 1.0
    )

    private val formulaMap = mapOf(
        "Total P+I (A)" to "A = P(1 + r/n)ⁿᵗ",
        "Principal (P) using A" to "P = A / (1 + r/n)ⁿᵗ",
        "Principal (P) using I" to "P = I / ((1 + r/n)ⁿᵗ – 1)",
        "Rate (R)" to "r = n × ((A/P)^(1/nt) – 1)",
        "Time (t)" to "t = ln(A/P) / n×ln(1 + r/n)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupSpinners()
        setupButtons()
        setupInputWatchers()
    }

    // ── Bind all views ─────────────────────────────────────────────────────────
    private fun bindViews() {
        spinnerCalculate = findViewById(R.id.spinnerCalculate)
        spinnerCompound = findViewById(R.id.spinnerCompound)
        tvFormula = findViewById(R.id.tvFormula)

        layoutPrincipal = findViewById(R.id.layoutPrincipal)
        layoutRate = findViewById(R.id.layoutRate)
        layoutTime = findViewById(R.id.layoutTime)
        layoutTotal = findViewById(R.id.layoutTotal)

        etPrincipal = findViewById(R.id.etPrincipal)
        etRate = findViewById(R.id.etRate)
        etTime = findViewById(R.id.etTime)
        etTotal = findViewById(R.id.etTotal)

        btnCalculate = findViewById(R.id.btnCalculate)
        btnClear = findViewById(R.id.btnClear)

        cardResult = findViewById(R.id.cardResult)
        tvResultLabel = findViewById(R.id.tvResultLabel)
        tvResultValue = findViewById(R.id.tvResultValue)
        tvPrincipal = findViewById(R.id.tvPrincipalResult)
        tvInterest = findViewById(R.id.tvInterestResult)
        tvSteps = findViewById(R.id.tvSteps)

        lineChart = findViewById(R.id.lineChart)

        cardInsight = findViewById(R.id.cardInsight)
        tvInsight = findViewById(R.id.tvInsight)
        tvWhatIf = findViewById(R.id.tvWhatIf)
    }

    // ── Spinners ───────────────────────────────────────────────────────────────
    private fun setupSpinners() {
        val calcAdapter = ArrayAdapter(this, R.layout.spinner_item, calculateOptions)
        calcAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerCalculate.adapter = calcAdapter

        val compAdapter = ArrayAdapter(this, R.layout.spinner_item, compoundOptions)
        compAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerCompound.adapter = compAdapter
        spinnerCompound.setSelection(6) // Default: Monthly

        spinnerCalculate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                updateFieldVisibility(calculateOptions[pos])
                tvFormula.text = formulaMap[calculateOptions[pos]] ?: ""
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun updateFieldVisibility(mode: String) {
        layoutPrincipal.visibility = View.VISIBLE
        layoutRate.visibility = View.VISIBLE
        layoutTime.visibility = View.VISIBLE
        layoutTotal.visibility = View.VISIBLE

        when (mode) {
            "Total P+I (A)" -> layoutTotal.visibility = View.GONE
            "Principal (P) using A" -> layoutPrincipal.visibility = View.GONE
            "Principal (P) using I" -> layoutPrincipal.visibility = View.GONE
            "Rate (R)" -> layoutRate.visibility = View.GONE
            "Time (t)" -> layoutTime.visibility = View.GONE
        }
    }

    // ── Buttons ────────────────────────────────────────────────────────────────
    private fun setupButtons() {
        btnCalculate.setOnClickListener {
            if (validateInputs()) {
                performCalculation()
            }
        }
        btnClear.setOnClickListener { clearAll() }
    }

    // ── Validation ─────────────────────────────────────────────────────────────
    private fun validateInputs(): Boolean {
        val mode = spinnerCalculate.selectedItem.toString()

        fun check(et: EditText, label: String): Boolean {
            val txt = et.text.toString().trim()
            if (txt.isEmpty()) { et.error = "$label is required"; return false }
            val v = txt.toDoubleOrNull()
            if (v == null) { et.error = "Enter a valid number"; return false }
            if (v < 0) { et.error = "$label cannot be negative"; return false }
            return true
        }

        fun checkPositive(et: EditText, label: String): Boolean {
            if (!check(et, label)) return false
            if (et.text.toString().toDouble() <= 0) { et.error = "$label must be > 0"; return false }
            return true
        }

        return when (mode) {
            "Total P+I (A)" -> checkPositive(etPrincipal, "Principal") &&
                    checkPositive(etRate, "Rate") &&
                    checkPositive(etTime, "Time")
            "Principal (P) using A" -> checkPositive(etTotal, "Total A") &&
                    checkPositive(etRate, "Rate") &&
                    checkPositive(etTime, "Time")
            "Principal (P) using I" -> checkPositive(etTotal, "Interest I") &&
                    checkPositive(etRate, "Rate") &&
                    checkPositive(etTime, "Time")
            "Rate (R)" -> checkPositive(etPrincipal, "Principal") &&
                    checkPositive(etTotal, "Total A") &&
                    checkPositive(etTime, "Time")
            "Time (t)" -> checkPositive(etPrincipal, "Principal") &&
                    checkPositive(etTotal, "Total A") &&
                    checkPositive(etRate, "Rate")
            else -> false
        }
    }

    // ── Core Calculation ───────────────────────────────────────────────────────
    private fun performCalculation() {
        val mode = spinnerCalculate.selectedItem.toString()
        val compoundKey = spinnerCompound.selectedItem.toString()
        val n = compoundN[compoundKey] ?: 12.0
        val isContinuous = n == -1.0

        val p = etPrincipal.text.toString().toDoubleOrNull() ?: 0.0
        val r = (etRate.text.toString().toDoubleOrNull() ?: 0.0) / 100.0
        val t = etTime.text.toString().toDoubleOrNull() ?: 0.0
        val a = etTotal.text.toString().toDoubleOrNull() ?: 0.0

        var resultValue = 0.0
        var stepsText = ""
        var principalVal = p
        var totalVal = a
        var rateVal = r
        var timeVal = t

        when (mode) {
            "Total P+I (A)" -> {
                resultValue = if (isContinuous) p * exp(r * t)
                else p * (1 + r / n).pow(n * t)
                totalVal = resultValue
                stepsText = buildStepsA(p, r, n, t, resultValue, isContinuous, compoundKey)
            }
            "Principal (P) using A" -> {
                resultValue = if (isContinuous) a / exp(r * t)
                else a / (1 + r / n).pow(n * t)
                principalVal = resultValue
                totalVal = a
                stepsText = "P = A / (1 + r/n)ⁿᵗ\nP = ${"%.2f".format(a)} / (1 + ${r}/${n})^(${n}×${t})\nP = ${"%.2f".format(resultValue)}"
            }
            "Principal (P) using I" -> {
                resultValue = if (isContinuous) a / (exp(r * t) - 1)
                else a / ((1 + r / n).pow(n * t) - 1)
                principalVal = resultValue
                stepsText = "P = I / ((1 + r/n)ⁿᵗ – 1)\nP = ${"%.2f".format(a)} / ((1+${r}/${n})^(${n}×${t}) – 1)\nP = ${"%.2f".format(resultValue)}"
                totalVal = resultValue + a
            }
            "Rate (R)" -> {
                resultValue = if (isContinuous) ln(a / p) / t
                else n * ((a / p).pow(1.0 / (n * t)) - 1)
                rateVal = resultValue
                stepsText = "r = n×((A/P)^(1/nt)–1)\nr = ${n}×((${"%.2f".format(a)}/${"%.2f".format(p)})^(1/(${n}×${t}))–1)\nr = ${"%.6f".format(resultValue)}\nR = ${"%.4f".format(resultValue * 100)}%"
            }
            "Time (t)" -> {
                resultValue = if (isContinuous) ln(a / p) / r
                else ln(a / p) / (n * ln(1 + r / n))
                timeVal = resultValue
                stepsText = "t = ln(A/P) / n×ln(1+r/n)\nt = ln(${"%.2f".format(a)}/${"%.2f".format(p)}) / ${n}×ln(1+${r}/${n})\nt = ${"%.4f".format(resultValue)} years"
            }
        }

        displayResult(mode, resultValue, principalVal, totalVal, rateVal, timeVal, stepsText)
        drawChart(principalVal, rateVal, timeVal, n, isContinuous)
        showInsight(principalVal, totalVal, rateVal, timeVal, n, isContinuous)

        cardResult.visibility = View.VISIBLE
        cardInsight.visibility = View.VISIBLE
        animateCardIn(cardResult)
        animateCardIn(cardInsight)
    }

    private fun buildStepsA(p: Double, r: Double, n: Double, t: Double, a: Double, cont: Boolean, label: String): String {
        return if (cont) {
            "A = P × eʳᵗ\nr = ${r}\nt = ${t}\nA = ${"%.2f".format(p)} × e^(${r}×${t})\nA = ${"%.2f".format(a)}"
        } else {
            "Step 1: Convert R to decimal\nr = ${r * 100}/100 = ${r}\n\nStep 2: Solve A = P(1 + r/n)ⁿᵗ\nA = ${"%.2f".format(p)} × (1 + ${r}/${n})^(${n}×${t})\nA = ${"%.2f".format(p)} × ${"%.8f".format((1 + r / n).pow(n * t))}\nA = ${"%.2f".format(a)}"
        }
    }

    // ── Display Result ─────────────────────────────────────────────────────────
    private fun displayResult(
        mode: String, result: Double,
        principal: Double, total: Double, rate: Double, time: Double,
        steps: String
    ) {
        val fmt = { v: Double -> "${"%.2f".format(v)}" }

        when (mode) {
            "Total P+I (A)" -> {
                tvResultLabel.text = "Total Amount (A)"
                tvResultValue.text = "$ ${fmt(result)}"
                tvPrincipal.text = "Principal (P): $ ${fmt(principal)}"
                tvInterest.text = "Interest (I): $ ${fmt(result - principal)}"
            }
            "Principal (P) using A", "Principal (P) using I" -> {
                tvResultLabel.text = "Principal (P)"
                tvResultValue.text = "$ ${fmt(result)}"
                tvPrincipal.text = "Total (A): $ ${fmt(total)}"
                tvInterest.text = "Interest (I): $ ${fmt(total - result)}"
            }
            "Rate (R)" -> {
                tvResultLabel.text = "Annual Rate (R)"
                tvResultValue.text = "${"%.4f".format(result * 100)} %"
                tvPrincipal.text = "Principal (P): $ ${fmt(principal)}"
                tvInterest.text = "Total (A): $ ${fmt(total)}"
            }
            "Time (t)" -> {
                tvResultLabel.text = "Time (t)"
                val years = result.toInt()
                val months = ((result - years) * 12).roundToInt()
                tvResultValue.text = "${"%.4f".format(result)} years"
                tvPrincipal.text = "≈ $years yr $months mo"
                tvInterest.text = "Total (A): $ ${fmt(total)}"
            }
        }
        tvSteps.text = steps
        animateValue(tvResultValue, result, mode)
    }

    // ── Animate counter ────────────────────────────────────────────────────────
    private fun animateValue(tv: TextView, target: Double, mode: String) {
        if (mode == "Rate (R)" || mode == "Time (t)") return // Skip animation for non-dollar outputs
        val animator = ValueAnimator.ofFloat(0f, target.toFloat())
        animator.duration = 800
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener {
            val v = it.animatedValue as Float
            tv.text = "$ ${"%.2f".format(v.toDouble())}"
        }
        animator.start()
    }

    // ── Line Chart ─────────────────────────────────────────────────────────────
    private fun drawChart(p: Double, r: Double, t: Double, n: Double, continuous: Boolean) {
        if (p <= 0 || r <= 0 || t <= 0) return

        val principalEntries = mutableListOf<Entry>()
        val growthEntries = mutableListOf<Entry>()
        val steps = 50
        val stepSize = t / steps

        for (i in 0..steps) {
            val ti = i * stepSize
            val a = if (continuous) p * exp(r * ti) else p * (1 + r / n).pow(n * ti)
            principalEntries.add(Entry(ti.toFloat(), p.toFloat()))
            growthEntries.add(Entry(ti.toFloat(), a.toFloat()))
        }

        val principalDS = LineDataSet(principalEntries, "Principal").apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.chart_principal)
            setDrawCircles(false)
            lineWidth = 2f
            setDrawFilled(false)
            enableDashedLine(10f, 5f, 0f)
            setDrawValues(false)
        }

        val growthDS = LineDataSet(growthEntries, "Total (P+I)").apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.chart_growth)
            setDrawCircles(false)
            lineWidth = 3f
            fillAlpha = 60
            fillColor = ContextCompat.getColor(this@MainActivity, R.color.chart_growth)
            setDrawFilled(true)
            setDrawValues(false)
        }

        lineChart.apply {
            data = LineData(principalDS, growthDS)
            description.isEnabled = false
            setTouchEnabled(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = ContextCompat.getColor(this@MainActivity, R.color.text_secondary)
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) = "${value.toInt()}yr"
            }
            axisLeft.textColor = ContextCompat.getColor(this@MainActivity, R.color.text_secondary)
            axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) =
                    if (value >= 1000) "$${(value / 1000).toInt()}k" else "$${"%.0f".format(value)}"
            }
            axisRight.isEnabled = false
            legend.textColor = ContextCompat.getColor(this@MainActivity, R.color.text_primary)
            animateX(600)
            invalidate()
        }
    }

    // ── Financial Insight ──────────────────────────────────────────────────────
    private fun showInsight(p: Double, a: Double, r: Double, t: Double, n: Double, continuous: Boolean) {
        if (p <= 0 || r <= 0 || t <= 0) return

        val interest = a - p
        val pct = if (p > 0) (interest / p) * 100 else 0.0

        val insight = buildString {
            appendLine("📊 Summary")
            appendLine("• Total interest earned: $${"%.2f".format(interest)}")
            appendLine("• That's ${"%.1f".format(pct)}% return on your principal")
            appendLine()
            appendLine("⚡ Compounding Effect")
            val nLabel = spinnerCompound.selectedItem.toString()
            appendLine("• Compounding $nLabel accelerates growth by reinvesting interest")

            // Compare with simple interest
            val simpleA = p * (1 + r * t)
            val extra = a - simpleA
            if (extra > 0) {
                appendLine("• vs Simple Interest: $${
                    "%.2f".format(simpleA)
                } — compounding earned you $${
                    "%.2f".format(extra)
                } more!")
            }
        }

        // What If: waited 5 more years
        val laterA = if (continuous) p * exp(r * (t + 5)) else p * (1 + r / n).pow(n * (t + 5))
        val whatIf = buildString {
            appendLine("💡 What If?")
            appendLine("If you waited 5 more years (${t + 5} yrs total):")
            appendLine("• You'd have: $${"%.2f".format(laterA)}")
            appendLine("• Extra gain: $${"%.2f".format(laterA - a)}")
            appendLine()
            val doubleTime = if (continuous) ln(2.0) / r else ln(2.0) / (n * ln(1 + r / n))
            appendLine("⏱ Rule of 72: At ${
                "%.2f".format(r * 100)
            }% rate, your money doubles every ~${"%.1f".format(doubleTime)} years")
        }

        tvInsight.text = insight
        tvWhatIf.text = whatIf
    }

    // ── Input Watchers (prevent letters) ──────────────────────────────────────
    private fun setupInputWatchers() {
        listOf(etPrincipal, etRate, etTime, etTotal).forEach { et ->
            et.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) { et.error = null }
            })
        }
    }

    // ── Clear ──────────────────────────────────────────────────────────────────
    private fun clearAll() {
        etPrincipal.text.clear()
        etRate.text.clear()
        etTime.text.clear()
        etTotal.text.clear()
        cardResult.visibility = View.GONE
        cardInsight.visibility = View.GONE
        lineChart.clear()
    }

    // ── Animate card ───────────────────────────────────────────────────────────
    private fun animateCardIn(view: View) {
        view.alpha = 0f
        view.translationY = 40f
        view.animate().alpha(1f).translationY(0f).setDuration(400).start()
    }

    private fun Double.roundToInt() = Math.round(this).toInt()
}
