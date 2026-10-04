import 'package:flutter_test/flutter_test.dart';
import 'package:si_pekat/main.dart';

void main() {
  testWidgets('shows login screen on startup', (WidgetTester tester) async {
    await tester.pumpWidget(const SiPekatApp(
      isLoggedIn: false,
      userEmail: '',
      userNama: '',
    ));
    await tester.pumpAndSettle();

    expect(find.text('SiPEKAT'), findsWidgets);
  });
}
