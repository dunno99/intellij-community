// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ui.dsl.builder.impl

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.CellBase
import com.intellij.ui.dsl.builder.SpacingConfiguration
import com.intellij.ui.dsl.checkNull
import com.intellij.ui.dsl.gridLayout.Constraints
import org.jetbrains.annotations.ApiStatus
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

@ApiStatus.Internal
internal abstract class PlaceholderBaseImpl<T : CellBase<T>>(private val parent: RowImpl) : CellBaseImpl<T>() {

  private var placeholderCellData: PlaceholderCellData? = null
  private var visible = true
  private var enabled = true

  private var componentField: JComponent? = null

  var component: JComponent?
    get() = componentField
    set(value) {
      if (componentField !== value) {
        removeComponent()
        if (value != null) {
          value.isVisible = visible && parent.isVisible()
          value.isEnabled = enabled && parent.isEnabled()

          componentField = value

          if (placeholderCellData != null) {
            initInstalledComponent()
          }
        }
      }
    }

  /**
   * The listener catches [component] removing from parent bypassing [component] property (e.g. with [JPanel.remove] or directly adding
   * the component into another panel). So we can reset [component] field to keep consistency
   */
  private var hierarchyListener: HierarchyListener? = null

  override fun enabledFromParent(parentEnabled: Boolean) {
    doEnabled(parentEnabled && enabled)
  }

  override fun enabled(isEnabled: Boolean): CellBase<T> {
    enabled = isEnabled
    if (parent.isEnabled()) {
      doEnabled(enabled)
    }
    return this
  }

  override fun visibleFromParent(parentVisible: Boolean) {
    doVisible(parentVisible && visible)
  }

  override fun visible(isVisible: Boolean): CellBase<T> {
    visible = isVisible
    if (parent.isVisible()) {
      doVisible(visible)
    }
    component?.isVisible = isVisible
    return this
  }

  open fun init(panel: DialogPanel, constraints: Constraints, spacing: SpacingConfiguration) {
    placeholderCellData = PlaceholderCellData(panel, constraints, spacing)
    if (componentField != null) {
      initInstalledComponent()
    }
  }

  private fun removeComponent() {
    val installedComponent = componentField

    if (installedComponent == null) {
      return
    }

    componentField = null

    hierarchyListener?.let {
      hierarchyListener = null

      // We cannot remove listener while hierarchyListener is processing hierarchyChanged event, otherwise
      // JDK can throw IndexOutOfBoundsException. So postpone it
      SwingUtilities.invokeLater {
        installedComponent.removeHierarchyListener(it)
      }
    }

    placeholderCellData?.let {
      if (installedComponent is DialogPanel) {
        it.panel.unregisterIntegratedPanel(installedComponent)
      }
      it.panel.remove(installedComponent)
      invalidate()
    }
  }

  private fun initInstalledComponent() {
    checkNull(hierarchyListener)
    checkNotNull(placeholderCellData)
    val installedComponent = checkNotNull(componentField)

    placeholderCellData?.let {
      val gaps = customGaps ?: getComponentGaps(it.constraints.gaps.left, it.constraints.gaps.right, installedComponent, it.spacing)
      it.constraints = it.constraints.copy(
        gaps = gaps,
        visualPaddings = prepareVisualPaddings(installedComponent.origin)
      )
      it.panel.add(installedComponent, it.constraints)
      if (installedComponent is DialogPanel) {
        it.panel.registerIntegratedPanel(installedComponent)
      }

      hierarchyListener = HierarchyListener { event ->
        if (event.changeFlags and HierarchyEvent.PARENT_CHANGED.toLong() != 0L
            && event.changedParent === it.panel) {
          removeComponent()
        }
      }
      installedComponent.addHierarchyListener(hierarchyListener)

      invalidate()
    }
  }

  private fun doVisible(isVisible: Boolean) {
    component?.let {
      if (it.isVisible != isVisible) {
        it.isVisible = isVisible
        invalidate()
      }
    }
  }

  private fun doEnabled(isEnabled: Boolean) {
    component?.let {
      it.isEnabled = isEnabled
    }
  }

  private fun invalidate() {
    placeholderCellData?.let {
      // Force parent to re-layout
      it.panel.revalidate()
      it.panel.repaint()
    }
  }
}

private data class PlaceholderCellData(val panel: DialogPanel, var constraints: Constraints, val spacing: SpacingConfiguration)
