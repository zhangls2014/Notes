# Login input UI design

The user canceled floating navigation and approved rounded outlined login fields. Start from master; retain the canceled trial only on its old branch.

Use Material 3 OutlinedTextField with theme large corners (20dp), floating Account/Password labels and outlineVariant for the idle border. Keep theme focus/error colors and leading icons. Share 32dp horizontal form margins, remove the extra account top padding, retain 16dp between fields. Render supporting text only for errors using default content alignment.

Use IconButton for clear/reveal actions with 48dp targets, interaction feedback and localized English/Chinese descriptions. Keep input validation, MVI, Next/Go, masking, IME handoff, scroll, two-pane and hinge rules. Buttons participate in keyboard traversal; verify reachability.

Validate actions and field identity with a focused UI test; run adaptive regressions and inspect compact/dark/large-font/error screenshots. Build and install Android to the existing emulator with its matching debug certificate, compile iOS.
