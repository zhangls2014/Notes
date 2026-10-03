import unittest

from generate import path_calls


class PathConversionTest(unittest.TestCase):
    def test_absolute_and_reflective_coordinates_use_positive_viewport(self):
        self.assertEqual(
            ['moveTo(10f, 20f)', 'quadTo(30f, 40f, 50f, 60f)',
             'reflectiveQuadTo(70f, 80f)', 'verticalLineTo(90f)', 'horizontalLineTo(100f)', 'close()'],
            path_calls('M10-940Q30-920 50-900T70-880V-870H100Z'),
        )

    def test_relative_commands_are_not_translated(self):
        self.assertEqual(
            ['moveTo(10f, 20f)', 'quadToRelative(1f, -2f, 3f, -4f)',
             'reflectiveQuadToRelative(5f, -6f)', 'verticalLineToRelative(-7f)',
             'close()', 'moveToRelative(8f, -9f)'],
            path_calls('m10-940q1-2 3-4t5-6v-7zm8-9'),
        )

    def test_repeated_move_coordinates_become_lines(self):
        self.assertEqual(
            ['moveTo(1f, 2f)', 'lineTo(3f, 4f)', 'moveToRelative(5f, -6f)', 'lineToRelative(7f, -8f)'],
            path_calls('M1-958 3-956m5-6 7-8'),
        )

    def test_cubic_control_points_are_translated(self):
        self.assertEqual(
            ['moveTo(0f, 0f)', 'curveTo(1f, 2f, 3f, 4f, 5f, 6f)',
             'reflectiveCurveTo(7f, 8f, 9f, 10f)'],
            path_calls('M0-960C1-958 3-956 5-954S7-952 9-950'),
        )

    def test_invalid_or_unsupported_input_is_rejected(self):
        for data in ['M1', 'L1 2', 'M0 0 A1 1 0 0 0 2 2', 'M0 0 @']:
            with self.subTest(data=data), self.assertRaises(ValueError):
                path_calls(data)


if __name__ == '__main__':
    unittest.main()
