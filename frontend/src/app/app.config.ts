import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { definePreset } from '@primeuix/themes';
import { MessageService, ConfirmationService } from 'primeng/api';
import { routes } from './app.routes';
import { problemInterceptor } from './core/interceptors/problem.interceptor';

const DocumentShieldPreset = definePreset(Aura, {
  primitive: {
    blue: {
      50: '#edf4ff',
      100: '#d8e7ff',
      200: '#b9d2ff',
      300: '#8bb4ff',
      400: '#5890f2',
      500: '#2f70df',
      600: '#1558c9',
      700: '#1249a8',
      800: '#123f87',
      900: '#12366d',
      950: '#0a1f43',
    },
  },
  semantic: {
    primary: {
      50: '{blue.50}',
      100: '{blue.100}',
      200: '{blue.200}',
      300: '{blue.300}',
      400: '{blue.400}',
      500: '{blue.500}',
      600: '{blue.600}',
      700: '{blue.700}',
      800: '{blue.800}',
      900: '{blue.900}',
      950: '{blue.950}',
    },
    colorScheme: {
      dark: {
        surface: {
          0: '#ffffff',
          50: '#f5f7fa',
          100: '#e1e6ed',
          200: '#c4ccd8',
          300: '#aeb8c7',
          400: '#929eae',
          500: '#778394',
          600: '#4b5868',
          700: '#293342',
          800: '#1d2530',
          900: '#151b24',
          950: '#030407',
        },
        primary: {
          color: '{blue.600}',
          contrastColor: '#ffffff',
          hoverColor: '{blue.500}',
          activeColor: '{blue.700}',
        },
        formField: {
          background: '#0f131a',
          borderColor: '#293342',
          hoverBorderColor: '#3a4759',
          focusBorderColor: '{blue.500}',
          color: '#f5f7fa',
          placeholderColor: '#929eae',
        },
        content: {
          background: '#151b24',
          hoverBackground: '#1d2530',
          borderColor: '#293342',
          color: '#f5f7fa',
          hoverColor: '#ffffff',
        },
      },
    },
  },
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([problemInterceptor]),
      withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' }),
    ),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: DocumentShieldPreset,
        options: { darkModeSelector: '.app-dark', cssLayer: { name: 'primeng', order: 'primeng' } },
      },
    }),
    MessageService,
    ConfirmationService,
  ],
};
