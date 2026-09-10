import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { routes } from './app.routes';

describe('Application routes', () => {
  let harness: RouterTestingHarness;
  let router: Router;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes)],
    });

    harness = await RouterTestingHarness.create();
    router = TestBed.inject(Router);
  });

  it('should redirect the root route to the dashboard', async () => {
    await harness.navigateByUrl('/');

    expect(router.url).toBe('/dashboard');
    expect(harness.routeNativeElement?.textContent).toContain('Dashboard');
  });

  it('should render the login page', async () => {
    await harness.navigateByUrl('/login');

    expect(router.url).toBe('/login');
    expect(harness.routeNativeElement?.textContent).toContain('Sign in');
  });

  it('should render the registration page', async () => {
    await harness.navigateByUrl('/register');

    expect(router.url).toBe('/register');
    expect(harness.routeNativeElement?.textContent).toContain('Create account');
  });

  it('should render the dashboard inside the application layout', async () => {
    await harness.navigateByUrl('/dashboard');

    expect(router.url).toBe('/dashboard');
    expect(harness.routeNativeElement?.textContent).toContain('After');
    expect(harness.routeNativeElement?.textContent).toContain('Dashboard');
  });

  it('should render the not found page for an unknown route', async () => {
    await harness.navigateByUrl('/this-route-does-not-exist');

    expect(harness.routeNativeElement?.textContent).toContain('404');
    expect(harness.routeNativeElement?.textContent).toContain('Page not found');
  });
});