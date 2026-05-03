package com.mist.refrigeratorreminder.ui.common

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.mist.refrigeratorreminder.R
import com.mist.refrigeratorreminder.domain.model.FreshnessStatus
import com.mist.refrigeratorreminder.domain.model.ItemCategory

object IngredientVisuals {
    @DrawableRes
    fun ingredientIconRes(name: String, category: ItemCategory): Int {
        val normalized = normalize(name)
        return when {
            normalized.contains("양파") || normalized.contains("마늘") || normalized.contains("샬롯") -> R.drawable.ic_ing_onion
            normalized.contains("대파") || normalized.contains("쪽파") || normalized == "파" || normalized.startsWith("파대파") -> R.drawable.ic_ing_scallion
            normalized.contains("당근") -> R.drawable.ic_ing_carrot
            normalized.contains("감자") || normalized.contains("고구마") || normalized.contains("무") -> R.drawable.ic_ing_root
            normalized.contains("버섯") -> R.drawable.ic_ing_mushroom
            normalized.contains("토마토") -> R.drawable.ic_ing_tomato
            normalized.contains("브로콜리") || normalized.contains("콜리플라워") || normalized.contains("양배추") -> R.drawable.ic_ing_cabbage
            normalized.contains("시금치") || normalized.contains("상추") || normalized.contains("깻잎") ||
                normalized.contains("배추") || normalized.contains("콩나물") || normalized.contains("숙주") -> R.drawable.ic_ing_leaf
            normalized.contains("오이") || normalized.contains("애호박") || normalized.contains("단호박") ||
                normalized.contains("가지") || normalized.contains("파프리카") || normalized.contains("피망") ||
                normalized.contains("고추") -> R.drawable.ic_ing_leaf
            normalized.contains("사과") || normalized.contains("배") || normalized.contains("복숭아") ||
                normalized.contains("자두") || normalized.contains("감") || normalized.contains("아보카도") ||
                normalized.contains("키위") -> R.drawable.ic_ing_fruit_round
            normalized.contains("오렌지") || normalized.contains("레몬") || normalized.contains("귤") -> R.drawable.ic_ing_citrus
            normalized.contains("딸기") || normalized.contains("블루베리") || normalized.contains("포도") -> R.drawable.ic_ing_berries
            normalized.contains("수박") || normalized.contains("멜론") || normalized.contains("파인애플") ||
                normalized.contains("망고") || normalized.contains("바나나") -> R.drawable.ic_ing_melon
            normalized.contains("우유") || normalized.contains("요거트") || normalized.contains("치즈") ||
                normalized.contains("버터") -> R.drawable.ic_ing_milk
            normalized.contains("계란") || normalized.contains("달걀") -> R.drawable.ic_ing_egg
            normalized.contains("두부") -> R.drawable.ic_ing_tofu
            normalized.contains("김치") || normalized.contains("반찬") -> R.drawable.ic_ing_bowl
            normalized.contains("간장") || normalized.contains("고추장") || normalized.contains("된장") ||
                normalized.contains("케첩") || normalized.contains("마요네즈") || normalized.contains("소스") ||
                normalized.contains("드레싱") -> R.drawable.ic_ing_sauce
            normalized.contains("돼지") || normalized.contains("소고기") || normalized.contains("닭고기") ||
                normalized.contains("닭가슴살") || normalized.contains("오리") || normalized.contains("삼겹살") ||
                normalized.contains("목살") -> R.drawable.ic_ing_meat
            normalized.contains("문어") || normalized.contains("오징어") || normalized.contains("연어") ||
                normalized.contains("고등어") || normalized.contains("갈치") || normalized.contains("명태") ||
                normalized.contains("멸치") || normalized.contains("생선") -> R.drawable.ic_ing_fish
            normalized.contains("새우") || normalized.contains("조개") || normalized.contains("바지락") ||
                normalized.contains("홍합") || normalized.contains("굴") || normalized.contains("게") -> R.drawable.ic_ing_shell
            category == ItemCategory.DRINK -> R.drawable.ic_ing_drink
            category == ItemCategory.SAUCE -> R.drawable.ic_ing_sauce
            category == ItemCategory.SIDE_DISH -> R.drawable.ic_ing_bowl
            category == ItemCategory.MEAT -> R.drawable.ic_ing_meat
            category == ItemCategory.SEAFOOD -> R.drawable.ic_ing_fish
            category == ItemCategory.DAIRY -> R.drawable.ic_ing_milk
            category == ItemCategory.FRUIT -> R.drawable.ic_ing_fruit_round
            category == ItemCategory.VEGETABLE -> R.drawable.ic_ing_leaf
            else -> R.drawable.ic_ing_etc
        }
    }

    @DrawableRes
    fun suggestionIconRes(name: String, category: ItemCategory, directAdd: Boolean): Int {
        if (directAdd) return R.drawable.ic_ing_add
        return ingredientIconRes(name, category)
    }

    @DrawableRes
    fun riskIconRes(status: FreshnessStatus): Int = when (status) {
        FreshnessStatus.EXPIRED -> R.drawable.ic_risk_expired
        FreshnessStatus.TODAY -> R.drawable.ic_risk_today
        FreshnessStatus.SOON -> R.drawable.ic_risk_soon
        FreshnessStatus.SAFE -> R.drawable.ic_risk_safe
        FreshnessStatus.CONSUMED,
        FreshnessStatus.DISCARDED,
        -> R.drawable.ic_risk_done
    }

    @ColorRes
    fun riskTintRes(status: FreshnessStatus): Int = when (status) {
        FreshnessStatus.EXPIRED -> R.color.status_expired_fg
        FreshnessStatus.TODAY -> R.color.status_today_fg
        FreshnessStatus.SOON -> R.color.status_soon_fg
        FreshnessStatus.SAFE -> R.color.status_safe_fg
        FreshnessStatus.CONSUMED,
        FreshnessStatus.DISCARDED,
        -> R.color.status_done_fg
    }

    @ColorRes
    fun statusForegroundRes(status: FreshnessStatus): Int = when (status) {
        FreshnessStatus.EXPIRED -> R.color.status_expired_fg
        FreshnessStatus.TODAY -> R.color.status_today_fg
        FreshnessStatus.SOON -> R.color.status_soon_fg
        FreshnessStatus.SAFE -> R.color.status_safe_fg
        FreshnessStatus.CONSUMED,
        FreshnessStatus.DISCARDED,
        -> R.color.status_done_fg
    }

    @ColorRes
    fun statusBackgroundRes(status: FreshnessStatus): Int = when (status) {
        FreshnessStatus.EXPIRED -> R.color.status_expired_bg
        FreshnessStatus.TODAY -> R.color.status_today_bg
        FreshnessStatus.SOON -> R.color.status_soon_bg
        FreshnessStatus.SAFE -> R.color.status_safe_bg
        FreshnessStatus.CONSUMED,
        FreshnessStatus.DISCARDED,
        -> R.color.status_done_bg
    }

    @ColorRes
    fun ingredientTintRes(category: ItemCategory): Int = when (category) {
        ItemCategory.VEGETABLE -> R.color.icon_vegetable_fg
        ItemCategory.FRUIT -> R.color.icon_fruit_fg
        ItemCategory.DAIRY -> R.color.icon_dairy_fg
        ItemCategory.MEAT -> R.color.icon_meat_fg
        ItemCategory.SEAFOOD -> R.color.icon_seafood_fg
        ItemCategory.DRINK -> R.color.icon_drink_fg
        ItemCategory.SIDE_DISH -> R.color.icon_side_dish_fg
        ItemCategory.SAUCE -> R.color.icon_sauce_fg
        ItemCategory.ETC -> R.color.icon_etc_fg
    }

    @ColorRes
    fun ingredientBackgroundRes(category: ItemCategory): Int = when (category) {
        ItemCategory.VEGETABLE -> R.color.icon_vegetable_bg
        ItemCategory.FRUIT -> R.color.icon_fruit_bg
        ItemCategory.DAIRY -> R.color.icon_dairy_bg
        ItemCategory.MEAT -> R.color.icon_meat_bg
        ItemCategory.SEAFOOD -> R.color.icon_seafood_bg
        ItemCategory.DRINK -> R.color.icon_drink_bg
        ItemCategory.SIDE_DISH -> R.color.icon_side_dish_bg
        ItemCategory.SAUCE -> R.color.icon_sauce_bg
        ItemCategory.ETC -> R.color.icon_etc_bg
    }

    private fun normalize(value: String): String {
        return value.lowercase().replace(Regex("[\\s\\-_/.,()]+"), "")
    }
}
