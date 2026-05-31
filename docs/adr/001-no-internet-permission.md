# No Internet Permission

Trail Sense will not declare the Internet permission in its manifest. This is an offline only application, and features need to be developed with that assumption. Many users are unable to access the Internet while on remote hikes.

## Status

accepted

## Consequences

- The app can't use the Internet
- Any models/maps need to be downloaded externally and imported or ship with the app
- Algorithms will need to be invented for features that usually are heavily reliant on the Internet and powerful servers