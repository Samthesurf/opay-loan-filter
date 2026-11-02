package com.samuel.opayloanfilter

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class FilterLogicTest {

  private val pkgs = setOf("team.opay.pay")
  private val kws = listOf("loan", "easemoni", "borrow")

  @Test
  fun opayLoanPromo_isBlocked() {
    assertTrue(shouldBlock("team.opay.pay", "Instant cash", "Get a loan from EaseMoni now", pkgs, kws))
  }

  @Test
  fun opayTransferAlert_isKept() {
    assertFalse(shouldBlock("team.opay.pay", "Transfer received", "You received NGN 5,000 from John", pkgs, kws))
  }

  @Test
  fun otherAppLoanText_isKept() {
    assertFalse(shouldBlock("com.whatsapp", "Loan offer", "Get a loan now", pkgs, kws))
  }

  @Test
  fun matchingIsCaseInsensitive() {
    assertTrue(shouldBlock("team.opay.pay", "LOAN", null, pkgs, kws))
  }
}
