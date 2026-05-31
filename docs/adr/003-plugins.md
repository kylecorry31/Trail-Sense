# Plugins

Plugin support will be added to allow users to extend Trail Sense's abilities beyond what I ship it with. This may include plugins that allow it to access permissions that Trail Sense itself does not have, such as Internet. Plugins will be separate APKs and use a Service to communicate with Trail Sense.

## Status

accepted

## Consequences

- Privacy and security concerns, need to let users opt-in to connecting to new plugins and verify signatures/permissions before sending or receiving data.
- Android may restrict this communication or change the permission system in a way that breaks plugins in the future