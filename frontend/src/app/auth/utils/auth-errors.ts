import { HttpErrorResponse } from '@angular/common/http';

export function loginErrorMessage(
  error: unknown,
): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Unable to sign in. Please try again.';
  }

  if (error.status === 0) {
    return 'Unable to reach the server.';
  }

  if (error.status === 401) {
    return 'Invalid email or password.';
  }

  if (error.status === 400) {
    return 'Please check the provided credentials.';
  }

  return 'Unable to sign in. Please try again.';
}

export function registerErrorMessage(
  error: unknown,
): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Unable to create the account. Please try again.';
  }

  if (error.status === 0) {
    return 'Unable to reach the server.';
  }

  if (error.status === 409) {
    return 'An account with this email already exists.';
  }

  if (error.status === 400) {
    return 'Please check the registration data.';
  }

  return 'Unable to create the account. Please try again.';
}