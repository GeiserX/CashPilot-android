# Security Policy

## Reporting Security Issues

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, please use GitHub's private vulnerability reporting:

1. Go to https://github.com/GeiserX/CashPilot-android/security/advisories
2. Click "Report a vulnerability"
3. Fill out the form with details

We will respond within **48 hours** and work with you to understand and address the issue.

### What to Include

- Type of issue (e.g., data leakage, insecure storage, authentication bypass)
- Full paths of affected source files
- Step-by-step instructions to reproduce
- Proof-of-concept or exploit code (if possible)
- Impact assessment and potential attack scenarios

### Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| Latest  | :white_check_mark: |

Only the latest version receives security updates. We recommend always running the latest version.

## Security Architecture

### Data Protection

- **Local-first** - All financial data stored on-device
- **No cloud sync** - Data never leaves the device unless explicitly exported
- **SQLite encryption** - Database protected at rest

### API Security

- **Dashboard communication** - Authenticated API calls to m4b-dashboard
- **No telemetry** - No data collection or phone-home functionality

## Security Best Practices for Users

1. **Keep the app updated** - Run the latest version from GitHub Releases
2. **Verify APK signatures** - Only install from official releases
3. **Use device encryption** - Enable full-disk encryption on your Android device
4. **Review permissions** - The app requests only necessary permissions

## Contact

For security questions that aren't vulnerabilities, contact: security@geiser.cloud
