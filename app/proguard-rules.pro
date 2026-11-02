# Opay Loan Filter ProGuard rules.
# Manifest-referenced components are kept by R8 automatically; the explicit
# keep below guards the notification listener against over-shrinking.
-keep public class com.samuel.opayloanfilter.LoanFilterListener {
  public protected *;
}
