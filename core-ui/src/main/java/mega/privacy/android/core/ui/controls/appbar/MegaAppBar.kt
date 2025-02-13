package mega.privacy.android.core.ui.controls.appbar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.menus.MenuActions
import mega.privacy.android.core.ui.controls.text.MarqueeText
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.badge
import mega.privacy.android.core.ui.theme.extensions.body4
import mega.privacy.android.core.ui.utils.composeLet

/**
 * Mega app bar.
 *
 * This component has been implemented following the latest rules for the new design system.
 * If this component does not serve your purpose, discuss this with the team in charge of
 * developing the new design system. It will be updated or override depending on the needs.
 *
 * @param appBarType [AppBarType]
 * @param title Title.
 * @param modifier [Modifier]
 * @param onNavigationPressed Action for navigation button.
 * @param badgeCount Count if should show a badge, null otherwise.
 * @param titleIcons Icons to show at the end of the title if any, null otherwise.
 * @param actions Available options.
 * @param onActionPressed Action for each available option.
 * @param maxActionsToShow The Max [actions] to be shown, if there are more they will be under three dots menu
 * @param enabled if false, the navigation icon and actions will be disabled
 * @param elevation Elevation.
 */
@Composable
fun MegaAppBar(
    appBarType: AppBarType,
    title: String,
    modifier: Modifier = Modifier,
    onNavigationPressed: (() -> Unit)? = null,
    badgeCount: Int? = null,
    titleIcons: @Composable (RowScope.() -> Unit)? = null,
    actions: List<MenuAction>? = null,
    onActionPressed: ((MenuAction) -> Unit)? = null,
    maxActionsToShow: Int = 4,
    enabled: Boolean = true,
    elevation: Dp = LocalMegaAppBarElevation.current,
) = CompositionLocalProvider(
    LocalMegaAppBarColors provides MegaAppBarColors(
        iconsTintColor = MegaTheme.colors.icon.primary,
        titleColor = MegaTheme.colors.text.primary,
        subtitleColor = MegaTheme.colors.text.secondary,
    )
) {
    BaseMegaAppBar(
        appBarType = appBarType,
        title = { MegaAppBarTitle(title) },
        modifier = modifier,
        onNavigationPressed = onNavigationPressed,
        badgeCount = badgeCount,
        titleIcons = titleIcons,
        actions = actions,
        onActionPressed = onActionPressed,
        maxActionsToShow = maxActionsToShow,
        enabled = enabled,
        elevation = elevation
    )
}

/**
 * Mega app bar.
 *
 * This component has been implemented following the latest rules for the new design system.
 * If this component does not serve your purpose, discuss this with the team in charge of
 * developing the new design system. It will be updated or override depending on the needs.
 *
 * @param appBarType [AppBarType]
 * @param title Title.
 * @param subtitle Subtitle.
 * @param modifier [Modifier]
 * @param onNavigationPressed Action for navigation button.
 * @param badgeCount Count if should show a badge, null otherwise.
 * @param titleIcons Icons to show at the end of the title if any, null otherwise.
 * @param marqueeSubtitle True if the subtitle view should be marquee, false otherwise.
 * @param actions Available options.
 * @param onActionPressed Action for each available option.
 * @param maxActionsToShow The Max [actions] to be shown, if there are more they will be under three dots menu
 * @param enabled if false, the navigation icon and actions will be disabled
 * @param elevation Elevation.
 */
@Composable
fun MegaAppBar(
    appBarType: AppBarType,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onNavigationPressed: (() -> Unit)? = null,
    badgeCount: Int? = null,
    titleIcons: @Composable (RowScope.() -> Unit)? = null,
    marqueeSubtitle: Boolean = false,
    actions: List<MenuAction>? = null,
    onActionPressed: ((MenuAction) -> Unit)? = null,
    maxActionsToShow: Int = 4,
    enabled: Boolean = true,
    elevation: Dp = LocalMegaAppBarElevation.current,
) = CompositionLocalProvider(
    LocalMegaAppBarColors provides MegaAppBarColors(
        iconsTintColor = MegaTheme.colors.icon.primary,
        titleColor = MegaTheme.colors.text.primary,
        subtitleColor = MegaTheme.colors.text.secondary,
    )
) {
    BaseMegaAppBar(
        appBarType = appBarType,
        title = { MegaAppBarTitle(title) },
        modifier = modifier,
        onNavigationPressed = onNavigationPressed,
        badgeCount = badgeCount,
        titleIcons = titleIcons,
        subtitle = {
            if (marqueeSubtitle) {
                MegaAppBarMarqueeSubTitle(subtitle)
            } else {
                MegaAppBarSubTitle(subtitle)
            }
        },
        actions = actions,
        onActionPressed = onActionPressed,
        maxActionsToShow = maxActionsToShow,
        enabled = enabled,
        elevation = elevation
    )
}

internal data class MegaAppBarColors(
    val iconsTintColor: Color,
    val titleColor: Color,
    val subtitleColor: Color = Color.Unspecified,
    val backgroundAlpha: Float = 1f,
)

internal val LocalMegaAppBarColors =
    compositionLocalOf { MegaAppBarColors(Color.Unspecified, Color.Unspecified) }

internal val LocalMegaAppBarElevation = compositionLocalOf { AppBarDefaults.TopAppBarElevation }

@Composable
internal fun BaseMegaAppBar(
    appBarType: AppBarType,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onNavigationPressed: (() -> Unit)? = null,
    badgeCount: Int? = null,
    titleIcons: @Composable (RowScope.() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    actions: List<MenuAction>? = null,
    onActionPressed: ((MenuAction) -> Unit)? = null,
    maxActionsToShow: Int = 4,
    enabled: Boolean = true,
    elevation: Dp = LocalMegaAppBarElevation.current,
) = BaseMegaAppBar(
    appBarType = appBarType,
    titleAndSubtitle = {
        MegaAppBarTitleAndSubtitle(
            title = title,
            titleIcons = titleIcons,
            subtitle = subtitle,
            modifier = Modifier.then(Modifier.padding(end = if (actions.isNullOrEmpty()) 12.dp else 0.dp))
        )
    },
    actions = actions,
    modifier = modifier,
    onNavigationPressed = onNavigationPressed,
    badgeCount = badgeCount,
    onActionPressed = onActionPressed,
    maxActionsToShow = maxActionsToShow,
    enabled = enabled,
    elevation = elevation
)

@Composable
internal fun BaseMegaAppBar(
    appBarType: AppBarType,
    titleAndSubtitle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: List<MenuAction>? = null,
    onNavigationPressed: (() -> Unit)? = null,
    badgeCount: Int? = null,
    onActionPressed: ((MenuAction) -> Unit)? = null,
    maxActionsToShow: Int = 4,
    enabled: Boolean = true,
    elevation: Dp = LocalMegaAppBarElevation.current,
) {
    TopAppBar(
        title = titleAndSubtitle,
        backgroundColor = MegaTheme.colors.background.pageBackground.copy(LocalMegaAppBarColors.current.backgroundAlpha),
        modifier = modifier.testTag(TEST_TAG_APP_BAR),
        navigationIcon = appBarType.takeIf { it != AppBarType.NONE }?.composeLet {
            Box {
                NavigationIcon(
                    onNavigationPressed = onNavigationPressed ?: {},
                    enabled = enabled,
                    iconId = when (appBarType) {
                        AppBarType.BACK_NAVIGATION -> R.drawable.ic_back
                        AppBarType.MENU -> R.drawable.ic_menu
                        else -> return@Box
                    },
                    modifier = Modifier.testTag(APP_BAR_BACK_BUTTON_TAG),
                )
                badgeCount?.let {
                    Box(
                        modifier = Modifier
                            .padding(3.dp)
                            .size(24.dp)
                            .padding(3.dp)
                            .background(
                                color = MegaTheme.colors.indicator.pink,
                                shape = CircleShape
                            )
                            .border(
                                2.dp,
                                MegaTheme.colors.background.pageBackground,
                                shape = CircleShape
                            )
                            .testTag(APP_BAR_BADGE)
                            .align(Alignment.TopEnd),
                    ) {
                        Text(
                            text = it.toString(),
                            modifier = Modifier.align(Alignment.Center),
                            color = MegaTheme.colors.text.inverse,
                            style = MaterialTheme.typography.badge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
        actions = {
            actions?.let {
                MenuActions(
                    actions = actions,
                    maxActionsToShow = maxActionsToShow,
                    enabled = enabled,
                    onActionClick = { action -> onActionPressed?.invoke(action) }
                )
            }
        },
        elevation = elevation
    )
}

@Composable
internal fun MegaAppBarTitle(title: String, modifier: Modifier = Modifier, maxLines: Int = 1) =
    Text(
        text = title,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        color = LocalMegaAppBarColors.current.titleColor,
        style = MaterialTheme.typography.subtitle1,
        modifier = modifier
    )

@Composable
internal fun MegaAppBarSubTitle(
    subtitle: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) = Text(
    text = subtitle,
    modifier = modifier,
    color = LocalMegaAppBarColors.current.subtitleColor.takeOrElse { MegaTheme.colors.text.secondary },
    maxLines = maxLines,
    style = MaterialTheme.typography.body4
)

@Composable
internal fun MegaAppBarMarqueeSubTitle(
    subtitle: String,
    modifier: Modifier = Modifier,
) = MarqueeText(
    text = subtitle,
    modifier = modifier,
    color = LocalMegaAppBarColors.current.subtitleColor.takeOrElse { MegaTheme.colors.text.secondary },
    style = MaterialTheme.typography.body4
)

@Composable
internal fun MegaAppBarTitleAndSubtitle(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    titleIcons: @Composable (RowScope.() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
) =
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            title()
            CompositionLocalProvider(
                LocalContentColor provides MegaTheme.colors.icon.secondary,
                LocalContentAlpha provides 1f,
            ) {
                titleIcons?.let { it() }
            }
        }
        subtitle?.let { it() }
    }


@Composable
private fun NavigationIcon(
    onNavigationPressed: (() -> Unit),
    enabled: Boolean,
    @DrawableRes iconId: Int,
    modifier: Modifier = Modifier,
) = IconButton(onClick = onNavigationPressed, enabled = enabled, modifier = modifier) {
    Icon(
        imageVector = ImageVector.vectorResource(id = iconId),
        contentDescription = "Navigation button",
        tint = LocalMegaAppBarColors.current.iconsTintColor
    )
}

@CombinedThemePreviews
@Composable
private fun MegaAppBarPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaAppBar(
            appBarType = AppBarType.MENU,
            title = "App bar title",
            badgeCount = 3,
            subtitle = "Subtitle",
            titleIcons = {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Default.Add,
                    contentDescription = "",
                )
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Default.Check,
                    contentDescription = "",
                )
            }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MegaAppBarPreviewWithoutSubtitle() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaAppBar(
            appBarType = AppBarType.MENU,
            title = "App bar title",
            badgeCount = 3,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MegaAppBarPreviewWithMarqueeSubtitle() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaAppBar(
            appBarType = AppBarType.MENU,
            title = "App bar title",
            subtitle = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua",
            marqueeSubtitle = true,
            badgeCount = 3,
        )
    }
}

internal const val APP_BAR_BACK_BUTTON_TAG = "appbar:button_back"
internal const val APP_BAR_BADGE = "appbar:text_badge"

/**
 * Test Tag App Bar
 */
const val TEST_TAG_APP_BAR = "appbar"

/**
 * Enum class defining the app bar types.
 */
enum class AppBarType {
    /**
     * Back Navigation
     */
    BACK_NAVIGATION,

    /**
     * Menu
     */
    MENU,

    /**
     * None
     */
    NONE
}