/**
 * Mini-program-only strings: the tab bar, mobile-specific pages and hints, and display text for timetable fields.
 * Deep-merged with groupA/B/C and shell / space / campus / console (modules shared with the web).
 */
export default {
  tab: {
    home: 'Home',
    courses: 'Courses',
    schedule: 'Schedule',
    console: 'Admin',
    messages: 'Messages',
    mine: 'Me',
  },

  mobile: {
    consoleNote:
      'Teaching records (enrolments, grades, attendance, homework, ratings), permissions and logs are managed in the web console on a computer.',
    warningNormal: 'Normal',
    file: {
      fromChat: 'Choose a file from WeChat chats',
      fromDevice: 'Choose a file on this device',
      fromAlbum: 'Choose a photo',
      uploading: 'Uploading {p}%',
      open: 'Open',
    },
    datetime: {
      date: 'Pick a date',
      time: 'Pick a time',
      incomplete: 'Please pick both a date and a time',
    },
  },

  // Display text for timetable fields (stored in Chinese in the database)
  timetable: {
    weekday: { 1: 'Mon', 2: 'Tue', 3: 'Wed', 4: 'Thu', 5: 'Fri', 6: 'Sat', 7: 'Sun' },
    weekdayLong: {
      1: 'Monday',
      2: 'Tuesday',
      3: 'Wednesday',
      4: 'Thursday',
      5: 'Friday',
      6: 'Saturday',
      7: 'Sunday',
    },
    segment: { 1: 'Period 1', 2: 'Period 2', 3: 'Period 3', 4: 'Period 4', 5: 'Period 5' },
    type: { required: 'Required', elective: 'Elective' },
  },
}
