package com.smartsimulator.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.widget.*
import android.graphics.Typeface

data class Choice(val text: String, val money: Int, val energy: Int, val reputation: Int, val next: Int)
data class Scene(val title: String, val story: String, val choices: List<Choice>)

class MainActivity : Activity() {
    private val scenes = listOf(
        Scene("Новая жизнь", "Ты переехал в большой город. В кармане 500 монет. Как начнёшь?", listOf(
            Choice("Пойти на собеседование", 150, -15, 5, 1),
            Choice("Начать своё дело", -200, -20, 10, 2),
            Choice("Изучить новую профессию", -80, -10, 8, 3))),
        Scene("Первый рабочий день", "Коллега просит помочь с важным проектом.", listOf(
            Choice("Помочь коллеге", 80, -20, 15, 4),
            Choice("Сосредоточиться на своей задаче", 140, -10, -5, 4))),
        Scene("Стартап", "Первый клиент недоволен результатом. Как поступишь?", listOf(
            Choice("Вернуть деньги", -100, -5, 20, 4),
            Choice("Переделать заказ", 100, -25, 10, 4))),
        Scene("Обучение", "Тебе предлагают стажировку без оплаты.", listOf(
            Choice("Согласиться ради опыта", 0, -20, 20, 4),
            Choice("Найти подработку", 110, -15, 0, 4))),
        Scene("Неожиданный поворот", "Появился шанс открыть небольшую студию. Рискнёшь?", listOf(
            Choice("Инвестировать в студию", -180, -15, 20, 5),
            Choice("Накопить финансовую подушку", 90, 10, 5, 5))),
        Scene("Итоги главы", "Твои решения изменили твою жизнь. Посмотри результаты и попробуй другой путь.", emptyList())
    )
    private var index = 0
    private var money = 500
    private var energy = 100
    private var reputation = 50
    private var steps = 0
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("progress", MODE_PRIVATE)
        index = prefs.getInt("index", 0).coerceIn(0, scenes.lastIndex)
        money = prefs.getInt("money", 500)
        energy = prefs.getInt("energy", 100)
        reputation = prefs.getInt("reputation", 50)
        steps = prefs.getInt("steps", 0)
        render()
    }

    private fun save() = getSharedPreferences("progress", MODE_PRIVATE).edit()
        .putInt("index", index).putInt("money", money).putInt("energy", energy)
        .putInt("reputation", reputation).putInt("steps", steps).apply()

    private fun text(s: String, size: Float, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = s; textSize = size; setTextColor(Color.rgb(25, 35, 55))
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(0, 12, 0, 12)
        }

    private fun button(label: String, action: () -> Unit) {
        val b = Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 16f
            setOnClickListener { action() }
        }
        root.addView(b, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 10 })
    }

    private fun render() {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(244, 247, 252)) }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 40)
        }
        scroll.addView(root)
        setContentView(scroll)
        root.addView(text("УМНЫЙ СИМУЛЯТОР", 26f, true))
        root.addView(text("Глава 1 • Решения создают будущее", 15f))
        root.addView(text("💰 $money     ⚡ $energy     ★ $reputation", 19f, true))
        root.addView(text("Принято решений: $steps", 14f))
        val scene = scenes[index]
        root.addView(text(scene.title, 24f, true))
        root.addView(text(scene.story, 18f))
        scene.choices.forEach { c ->
            button(c.text) {
                money += c.money
                energy = (energy + c.energy).coerceIn(0, 100)
                reputation = (reputation + c.reputation).coerceIn(0, 100)
                index = c.next
                steps++
                save()
                render()
            }
        }
        if (scene.choices.isEmpty()) {
            root.addView(text("Финал: " + when {
                money >= 500 && reputation >= 70 -> "Успешный стратег"
                reputation >= 70 -> "Лидер сообщества"
                money >= 500 -> "Финансовый прагматик"
                else -> "Смелый экспериментатор"
            }, 20f, true))
            button("Пройти историю заново") {
                index = 0; money = 500; energy = 100; reputation = 50; steps = 0
                save(); render()
            }
        }
    }
}
