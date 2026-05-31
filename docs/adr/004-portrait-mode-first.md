## Portrait Mode First

Trail Sense will support the portrait mode screen orientation. Landscape may be forcefully enabled by Android, but it is not considered an officially supported way to use Trail Sense. Trail Sense is designed for use on phones in the outdoors, so tablet (and not foldables) support was not considered. It would take a significant effort to support both orientations and landscape mode on the phone would be suboptimal for most tools.

Changes required to comply with Android landscape mode policies will go with the easiest solution. This may involve leveraging global state or an ugly UI in landscape mode.

## Status

accepted

## Consequences

- Users hiking/camping with foldables or tablets will not be able to leverage their device in a way that is best for them
- Android may force apps to support landscape on all devices in the future
- Code supporting landscape mode policies may be ugly