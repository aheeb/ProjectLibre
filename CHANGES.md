# Changes in this fork

This is a modified version of ProjectLibre. The original is available at
https://github.com/claur/ProjectLibre and http://www.projectlibre.com.
This fork is not made, endorsed or supported by ProjectLibre, Inc.

The fork is based on upstream commit `0530be2` (ProjectLibre 1.9.8, 2025-04-30).
The list below summarizes the git history since then; the commits hold the details.

## 2026-11-05

- Windows builds for x64 and arm64 in GitHub Actions (`.github/workflows/windows.yml`), with updated Windows
  packaging scripts.

## 2026-11-02

### Look and feel
- Switched to FlatLaf; added a modern SVG icon set for commands and views.
- Replaced the Flamingo ribbon with a one-row toolbar (logo, project name, view switcher, view commands, zoom,
  search, share, help) and a complete menu bar. The ProjectLibre logo stays at the left of the toolbar
  (macOS) or the in-window menu bar (Windows and Linux) and links to projectlibre.com.
- macOS: screen menu bar, toolbar in the title bar, native open/save panel, consistent keyboard shortcuts.
- Narrow windows turn the view switcher into a labelled drop-down.
- Modern Gantt bars, task grid and timescale header; the same visual language for usage, network, WBS/RBS and
  chart views.
- Redesigned About dialog; reworded and re-laid-out dialogs (Task and Resource Information, Assign Resources,
  Project Information, Change Working Calendar, Find, Baseline, Update, links, language, print preview,
  Insert Column), with platform button order and consistent margins.

### Startup and windows
- Removed the startup popups (license click-through, e-mail prompt, tips).
- Added a start screen with new, open, recent and sample projects; New Project asks for name and start date.
- The main window is sized to the screen and remembers its bounds.
- Help offers a plain way into the user guide instead of a cloud trial advertisement.

### Editing
- Task grid: '+ Add a task' row, newcomer-friendly columns, inline 'Assigned to', right-click menu,
  Cmd+] / Cmd+[ to indent and outdent, paste one value into many cells, fill down (Cmd/Ctrl+D), single-step undo
  and redo for typed assignments and links.
- 'Depends on' accepts task names; Insert Column can create a named custom column and lists the most used
  fields first.
- Gantt: drag feedback, snapping to days, explained date pins, links can be dropped anywhere on a task's row.
- People list: newcomer columns, '+ Add a person', right-click menu with days off; working time editor lets you
  drag across a vacation.
- Workload: shows who is over 100%, when and on which tasks; the histogram treats 100% as full time and marks
  overallocation in red.
- Work is shown in days by default, with a setting for the unit.
- Project Information > Statistics shows task counts and who has work.
- Deleting rows no longer asks for confirmation when undo can restore them.
- Many fixes to scheduling and assignment behaviour (default durations, keeping work when people are assigned or
  replaced, part-time availability, 'Start After Previous Task' for phases).

### Files
- Microsoft Project import and export: keeps custom column names, calendars, vacations and days off; export
  writes a copy and keeps the .pod open; Save As with an .mpp name writes a Microsoft Project file.
- Opening a file no longer marks the project as modified or renames columns in other open projects.
- PDF export is one page wide by default, saved where you chose.

### Performance and packaging
- Faster Gantt painting and grid cell lookups; the Groovy action list compiles off the event thread.
- The main window is built on the event dispatch thread; removed explicit `System.gc()` calls; quieter default
  logging.
- Launchers size the heap relative to RAM and use G1 and class data sharing.
- macOS packaging: per-architecture apps with a minimal, CDS-enabled runtime.
- Development-only tooling to drive the running app and to check event dispatch thread use.
