import {
  ComponentFixture,
  TestBed,
} from '@angular/core/testing';
import {
  ActivatedRoute,
  Router,
  convertToParamMap,
} from '@angular/router';
import {
  of,
  throwError,
} from 'rxjs';
import { vi } from 'vitest';

import {
  CapsuleDetail,
  CapsuleParticipant,
  Contribution,
} from '../../models/capsule.models';
import { CapsuleService } from '../../services/capsule.service';
import { CapsuleDetailPage } from './capsule-detail';

describe('CapsuleDetailPage', () => {
  let component:
    CapsuleDetailPage;

  let fixture:
    ComponentFixture<CapsuleDetailPage>;

  let capsuleServiceSpy: {
    getCapsule:
      ReturnType<typeof vi.fn>;

    updateCapsule:
      ReturnType<typeof vi.fn>;

    getParticipants:
      ReturnType<typeof vi.fn>;

    createInvitation:
      ReturnType<typeof vi.fn>;

    getTextContributions:
      ReturnType<typeof vi.fn>;

    createTextContribution:
      ReturnType<typeof vi.fn>;

    updateTextContribution:
      ReturnType<typeof vi.fn>;

    deleteContribution:
      ReturnType<typeof vi.fn>;
  };

  let routerSpy: {
    navigate:
      ReturnType<typeof vi.fn>;
  };

  const collectingOwner:
    CapsuleDetail = {
      id:
        'capsule-123',

      title:
        'Future memories',

      description:
        'A description',

      type:
        'PERSONAL',

      status:
        'COLLECTING',

      opensAt:
        new Date(
          Date.now() +
            86_400_000,
        ).toISOString(),

      timezone:
        'Europe/Madrid',

      sealedAt:
        null,

      openedAt:
        null,

      createdAt:
        '2026-09-20T10:00:00Z',

      updatedAt:
        '2026-09-20T10:00:00Z',

      role:
        'OWNER',
    };

  const sharedOwner:
    CapsuleDetail = {
      ...collectingOwner,

      type:
        'SHARED',
    };

  const participants:
    CapsuleParticipant[] = [
      {
        email:
          'owner@example.com',

        role:
          'OWNER',

        joinedAt:
          '2026-09-20T10:00:00Z',
      },
      {
        email:
          'contributor@example.com',

        role:
          'CONTRIBUTOR',

        joinedAt:
          '2026-09-21T10:00:00Z',
      },
    ];

  const contribution:
    Contribution = {
      id:
        'contribution-1',

      capsuleId:
        'capsule-123',

      authorUserId:
        'user-123',

      type:
        'TEXT',

      textContent:
        'Future message',

      createdAt:
        '2026-09-23T12:00:00Z',

      updatedAt:
        '2026-09-23T12:00:00Z',
    };

  beforeEach(async () => {
    capsuleServiceSpy = {
      getCapsule:
        vi.fn(),

      updateCapsule:
        vi.fn(),

      getParticipants:
        vi.fn(),

      createInvitation:
        vi.fn(),

      getTextContributions:
        vi.fn(),

      createTextContribution:
        vi.fn(),

      updateTextContribution:
        vi.fn(),

      deleteContribution:
        vi.fn(),
    };

    routerSpy = {
      navigate:
        vi.fn(),
    };

    capsuleServiceSpy
      .getCapsule
      .mockReturnValue(
        of(
          collectingOwner,
        ),
      );

    capsuleServiceSpy
      .getParticipants
      .mockReturnValue(
        of(participants),
      );

    capsuleServiceSpy
      .getTextContributions
      .mockReturnValue(
        of([]),
      );

    await TestBed
      .configureTestingModule({
        imports: [
          CapsuleDetailPage,
        ],

        providers: [
          {
            provide:
              CapsuleService,

            useValue:
              capsuleServiceSpy,
          },

          {
            provide:
              ActivatedRoute,

            useValue: {
              snapshot: {
                paramMap:
                  convertToParamMap({
                    id:
                      'capsule-123',
                  }),
              },
            },
          },

          {
            provide:
              Router,

            useValue:
              routerSpy,
          },
        ],
      })
      .compileComponents();

    fixture =
      TestBed.createComponent(
        CapsuleDetailPage,
      );

    component =
      fixture.componentInstance;
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  function render(): void {
    fixture.detectChanges();
  }

  function toDateInputValue(
    date: Date,
  ): string {
    const year =
      date.getFullYear();

    const month =
      String(
        date.getMonth() + 1,
      ).padStart(2, '0');

    const day =
      String(
        date.getDate(),
      ).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  it('should load and render capsule detail', () => {
    render();

    expect(
      capsuleServiceSpy
        .getCapsule,
    ).toHaveBeenCalledWith(
      'capsule-123',
    );

    expect(
      fixture.nativeElement
        .textContent,
    ).toContain(
      'Future memories',
    );
  });

  it('should show an error when loading fails', () => {
    capsuleServiceSpy
      .getCapsule
      .mockReturnValue(
        throwError(
          () =>
            new Error(
              'Request failed',
            ),
        ),
      );

    render();

    expect(
      component.loadError(),
    ).toContain(
      'No se ha podido cargar',
    );

    expect(
      fixture.nativeElement
        .textContent,
    ).toContain(
      'No se pudo cargar la cápsula',
    );
  });

  it('should open the edit modal for an owner collecting capsule', () => {
    render();

    expect(
      component.canEdit(),
    ).toBe(true);

    component.startEditing();
    fixture.detectChanges();

    expect(
      component.isEditing(),
    ).toBe(true);

    expect(
      fixture.nativeElement
        .querySelector(
          '[role="dialog"]',
        ),
    ).toBeTruthy();

    expect(
      fixture.nativeElement
        .textContent,
    ).toContain(
      'Editar cápsula',
    );
  });

  it('should preload current values when editing starts', () => {
    render();

    component.startEditing();

    const values =
      component.editForm
        .getRawValue();

    expect(
      values.title,
    ).toBe(
      collectingOwner.title,
    );

    expect(
      values.description,
    ).toBe(
      collectingOwner.description,
    );

    expect(
      values.timezone,
    ).toBe(
      collectingOwner.timezone,
    );

    expect(
      values.date,
    ).toBeTruthy();

    expect(
      values.time,
    ).toBeTruthy();
  });

  it('should close the edit modal when cancelled', () => {
    render();

    component.startEditing();

    component.cancelEditing();

    expect(
      component.isEditing(),
    ).toBe(false);
  });

  it('should close the edit modal with Escape', () => {
    render();

    component.startEditing();

    component.onEscapePressed();

    expect(
      component.isEditing(),
    ).toBe(false);
  });

  it('should not allow a contributor to edit the capsule', () => {
    capsuleServiceSpy
      .getCapsule
      .mockReturnValue(
        of({
          ...collectingOwner,

          role:
            'CONTRIBUTOR',
        }),
      );

    render();

    expect(
      component.canEdit(),
    ).toBe(false);

    component.startEditing();

    expect(
      component.isEditing(),
    ).toBe(false);
  });

  it('should not allow editing a sealed capsule', () => {
    capsuleServiceSpy
      .getCapsule
      .mockReturnValue(
        of({
          ...collectingOwner,

          status:
            'SEALED',
        }),
      );

    render();

    expect(
      component.canEdit(),
    ).toBe(false);

    expect(
      fixture.nativeElement
        .textContent,
    ).toContain(
      'Cápsula sellada',
    );
  });

  it('should not allow editing an opened capsule', () => {
    capsuleServiceSpy
      .getCapsule
      .mockReturnValue(
        of({
          ...collectingOwner,

          status:
            'OPENED',
        }),
      );

    render();

    expect(
      component.canEdit(),
    ).toBe(false);

    expect(
      fixture.nativeElement
        .textContent,
    ).toContain(
      'Cápsula abierta',
    );
  });

  it('should load participants for a shared capsule', () => {
    capsuleServiceSpy
      .getCapsule
      .mockReturnValue(
        of(sharedOwner),
      );

    render();

    expect(
      capsuleServiceSpy
        .getParticipants,
    ).toHaveBeenCalledWith(
      'capsule-123',
    );

    expect(
      component.participants(),
    ).toEqual(
      participants,
    );
  });

  it('should render shared capsule participants', () => {
    capsuleServiceSpy
      .getCapsule
      .mockReturnValue(
        of(sharedOwner),
      );

    render();

    const text =
      fixture.nativeElement
        .textContent;

    expect(text).toContain(
      'Participantes',
    );

    expect(text).toContain(
      'owner@example.com',
    );

    expect(text).toContain(
      'contributor@example.com',
    );

    expect(text).toContain(
      'Propietario',
    );

    expect(text).toContain(
      'Colaborador',
    );
  });

  it('should not load participants for a personal capsule', () => {
    render();

    expect(
      capsuleServiceSpy
        .getParticipants,
    ).not.toHaveBeenCalled();

    expect(
      fixture.nativeElement
        .textContent,
    ).not.toContain(
      'Participantes',
    );
  });

  it('should clear participants when loading participants fails', () => {
    capsuleServiceSpy
      .getCapsule
      .mockReturnValue(
        of(sharedOwner),
      );

    capsuleServiceSpy
      .getParticipants
      .mockReturnValue(
        throwError(
          () =>
            new Error(
              'Participants failed',
            ),
        ),
      );

    render();

    expect(
      component.participants(),
    ).toEqual([]);

    expect(
      component.participantsLoading(),
    ).toBe(false);
  });

  it('should allow a collecting shared owner to invite participants', () => {
    capsuleServiceSpy
      .getCapsule
      .mockReturnValue(
        of(sharedOwner),
      );

    render();

    expect(
      component.canInvite(),
    ).toBe(true);

    expect(
      fixture.nativeElement
        .textContent,
    ).toContain(
      'Generar invitación',
    );
  });

  it('should not allow a contributor to generate invitations', () => {
    capsuleServiceSpy
      .getCapsule
      .mockReturnValue(
        of({
          ...sharedOwner,

          role:
            'CONTRIBUTOR',
        }),
      );

    render();

    expect(
      component.canInvite(),
    ).toBe(false);

    expect(
      fixture.nativeElement
        .textContent,
    ).not.toContain(
      'Generar invitación',
    );
  });

  it('should not allow invitations for a personal capsule', () => {
    render();

    expect(
      component.canInvite(),
    ).toBe(false);

    expect(
      fixture.nativeElement
        .textContent,
    ).not.toContain(
      'Generar invitación',
    );
  });

  it('should not allow invitations for a sealed shared capsule', () => {
    capsuleServiceSpy
      .getCapsule
      .mockReturnValue(
        of({
          ...sharedOwner,

          status:
            'SEALED',
        }),
      );

    render();

    expect(
      component.canInvite(),
    ).toBe(false);

    expect(
      fixture.nativeElement
        .textContent,
    ).not.toContain(
      'Generar invitación',
    );
  });

  it('should generate an invitation URL', () => {
    capsuleServiceSpy
      .getCapsule
      .mockReturnValue(
        of(sharedOwner),
      );

    capsuleServiceSpy
      .createInvitation
      .mockReturnValue(
        of({
          token:
            'invitation-token',
        }),
      );

    render();

    component.generateInvitation();

    expect(
      capsuleServiceSpy
        .createInvitation,
    ).toHaveBeenCalledWith(
      'capsule-123',
    );

    expect(
      component.invitationUrl(),
    ).toBe(
      `${window.location.origin}/invitations/invitation-token`,
    );

    expect(
      component.invitationLoading(),
    ).toBe(false);
  });

  it('should show an error when invitation generation fails', () => {
    capsuleServiceSpy
      .getCapsule
      .mockReturnValue(
        of(sharedOwner),
      );

    capsuleServiceSpy
      .createInvitation
      .mockReturnValue(
        throwError(
          () =>
            new Error(
              'Invitation failed',
            ),
        ),
      );

    render();

    component.generateInvitation();

    expect(
      component.invitationError(),
    ).toContain(
      'No se ha podido generar',
    );

    expect(
      component.invitationLoading(),
    ).toBe(false);
  });

  it('should copy the generated invitation URL', async () => {
    capsuleServiceSpy
      .getCapsule
      .mockReturnValue(
        of(sharedOwner),
      );

    capsuleServiceSpy
      .createInvitation
      .mockReturnValue(
        of({
          token:
            'invitation-token',
        }),
      );

    const writeText =
      vi.fn()
        .mockResolvedValue(
          undefined,
        );

    Object.defineProperty(
      navigator,
      'clipboard',
      {
        configurable: true,

        value: {
          writeText,
        },
      },
    );

    render();

    component.generateInvitation();

    await component.copyInvitation();

    expect(
      writeText,
    ).toHaveBeenCalledWith(
      `${window.location.origin}/invitations/invitation-token`,
    );

    expect(
      component.invitationCopied(),
    ).toBe(true);
  });

  it('should load own text contributions', () => {
    capsuleServiceSpy
      .getTextContributions
      .mockReturnValue(
        of([
          contribution,
        ]),
      );

    render();

    expect(
      capsuleServiceSpy
        .getTextContributions,
    ).toHaveBeenCalledWith(
      'capsule-123',
    );

    expect(
      component.contributions(),
    ).toEqual([
      contribution,
    ]);

    expect(
      fixture.nativeElement
        .textContent,
    ).toContain(
      'Future message',
    );
  });

  it('should show the empty contribution state', () => {
    render();

    expect(
      component.contributions(),
    ).toEqual([]);

    expect(
      fixture.nativeElement
        .textContent,
    ).toContain(
      'Todavía no has escrito ningún mensaje',
    );
  });

  it('should create a text contribution', () => {
    const created:
      Contribution = {
        ...contribution,

        textContent:
          'New message',
    };

    capsuleServiceSpy
      .createTextContribution
      .mockReturnValue(
        of(created),
      );

    render();

    component.contributionForm
      .setValue({
        textContent:
          'New message',
      });

    component.createContribution();

    expect(
      capsuleServiceSpy
        .createTextContribution,
    ).toHaveBeenCalledWith(
      'capsule-123',
      {
        textContent:
          'New message',
      },
    );

    expect(
      component.contributions(),
    ).toEqual([
      created,
    ]);

    expect(
      component.contributionSaved(),
    ).toBe(true);

    expect(
      component.contributionForm
        .controls
        .textContent.value,
    ).toBe('');
  });

  it('should reject an empty text contribution', () => {
    render();

    component.contributionForm
      .setValue({
        textContent:
          '   ',
      });

    component.createContribution();

    expect(
      capsuleServiceSpy
        .createTextContribution,
    ).not.toHaveBeenCalled();

    expect(
      component.contributionForm
        .controls
        .textContent.invalid,
    ).toBe(true);
  });

  it('should show an error when creating a contribution fails', () => {
    capsuleServiceSpy
      .createTextContribution
      .mockReturnValue(
        throwError(
          () =>
            new Error(
              'Create failed',
            ),
        ),
      );

    render();

    component.contributionForm
      .setValue({
        textContent:
          'New message',
      });

    component.createContribution();

    expect(
      component.contributionError(),
    ).toContain(
      'No se ha podido guardar',
    );

    expect(
      component.contributionSaving(),
    ).toBe(false);
  });

  it('should start editing a contribution with its current text', () => {
    capsuleServiceSpy
      .getTextContributions
      .mockReturnValue(
        of([
          contribution,
        ]),
      );

    render();

    component
      .startContributionEditing(
        contribution,
      );

    expect(
      component
        .editingContributionId(),
    ).toBe(
      'contribution-1',
    );

    expect(
      component
        .contributionEditForm
        .controls
        .textContent.value,
    ).toBe(
      'Future message',
    );
  });

  it('should edit a text contribution', () => {
    const updated:
      Contribution = {
        ...contribution,

        textContent:
          'Updated message',

        updatedAt:
          '2026-09-23T12:05:00Z',
    };

    capsuleServiceSpy
      .getTextContributions
      .mockReturnValue(
        of([
          contribution,
        ]),
      );

    capsuleServiceSpy
      .updateTextContribution
      .mockReturnValue(
        of(updated),
      );

    render();

    component
      .startContributionEditing(
        contribution,
      );

    component
      .contributionEditForm
      .setValue({
        textContent:
          'Updated message',
      });

    component.saveContribution(
      contribution.id,
    );

    expect(
      capsuleServiceSpy
        .updateTextContribution,
    ).toHaveBeenCalledWith(
      'capsule-123',
      'contribution-1',
      {
        textContent:
          'Updated message',
      },
    );

    expect(
      component.contributions()[0]
        .textContent,
    ).toBe(
      'Updated message',
    );

    expect(
      component
        .editingContributionId(),
    ).toBeNull();

    expect(
      component.contributionSaved(),
    ).toBe(true);
  });

  it('should show an error when editing a contribution fails', () => {
    capsuleServiceSpy
      .getTextContributions
      .mockReturnValue(
        of([
          contribution,
        ]),
      );

    capsuleServiceSpy
      .updateTextContribution
      .mockReturnValue(
        throwError(
          () =>
            new Error(
              'Update failed',
            ),
        ),
      );

    render();

    component
      .startContributionEditing(
        contribution,
      );

    component
      .contributionEditForm
      .setValue({
        textContent:
          'Updated message',
      });

    component.saveContribution(
      contribution.id,
    );

    expect(
      component.contributionError(),
    ).toContain(
      'No se ha podido actualizar',
    );

    expect(
      component
        .editingContributionId(),
    ).toBe(
      'contribution-1',
    );
  });

  it('should delete a text contribution', () => {
    capsuleServiceSpy
      .getTextContributions
      .mockReturnValue(
        of([
          contribution,
        ]),
      );

    capsuleServiceSpy
      .deleteContribution
      .mockReturnValue(
        of(undefined),
      );

    render();

    component.deleteContribution(
      contribution.id,
    );

    expect(
      capsuleServiceSpy
        .deleteContribution,
    ).toHaveBeenCalledWith(
      'capsule-123',
      'contribution-1',
    );

    expect(
      component.contributions(),
    ).toEqual([]);

    expect(
      component
        .deletingContributionId(),
    ).toBeNull();
  });

  it('should show an error when deleting a contribution fails', () => {
    capsuleServiceSpy
      .getTextContributions
      .mockReturnValue(
        of([
          contribution,
        ]),
      );

    capsuleServiceSpy
      .deleteContribution
      .mockReturnValue(
        throwError(
          () =>
            new Error(
              'Delete failed',
            ),
        ),
      );

    render();

    component.deleteContribution(
      contribution.id,
    );

    expect(
      component.contributions(),
    ).toEqual([
      contribution,
    ]);

    expect(
      component.contributionError(),
    ).toContain(
      'No se ha podido eliminar',
    );

    expect(
      component
        .deletingContributionId(),
    ).toBeNull();
  });

  it('should show an error when contributions fail to load', () => {
    capsuleServiceSpy
      .getTextContributions
      .mockReturnValue(
        throwError(
          () =>
            new Error(
              'Load failed',
            ),
        ),
      );

    render();

    expect(
      component.contributions(),
    ).toEqual([]);

    expect(
      component.contributionError(),
    ).toContain(
      'No se han podido cargar',
    );

    expect(
      component
        .contributionsLoading(),
    ).toBe(false);
  });

  it('should allow a contributor to manage own contributions while collecting', () => {
    capsuleServiceSpy
      .getCapsule
      .mockReturnValue(
        of({
          ...sharedOwner,

          role:
            'CONTRIBUTOR',
        }),
      );

    render();

    expect(
      component
        .canManageContributions(),
    ).toBe(true);

    expect(
      fixture.nativeElement
        .textContent,
    ).toContain(
      'Guardar mensaje',
    );
  });

  it('should hide contribution actions when capsule is sealed', () => {
    capsuleServiceSpy
      .getCapsule
      .mockReturnValue(
        of({
          ...collectingOwner,

          status:
            'SEALED',
        }),
      );

    capsuleServiceSpy
      .getTextContributions
      .mockReturnValue(
        of([
          contribution,
        ]),
      );

    render();

    expect(
      component
        .canManageContributions(),
    ).toBe(false);

    const text =
      fixture.nativeElement
        .textContent;

    expect(text).not.toContain(
      'Guardar mensaje',
    );

    expect(text).not.toContain(
      'Editar',
    );

    expect(text).not.toContain(
      'Eliminar',
    );

    expect(text).toContain(
      'Future message',
    );
  });

  it('should move the edit date to tomorrow when a past time is selected for today', () => {
    vi.useFakeTimers();

    vi.setSystemTime(
      new Date(
        2026,
        8,
        23,
        12,
        0,
        0,
      ),
    );

    render();

    component.startEditing();

    component.editForm.patchValue({
      date:
        '2026-09-23',

      time:
        '10:00',
    });

    component.onEditTimeChange();

    expect(
      component.editForm.controls
        .date.value,
    ).toBe(
      '2026-09-24',
    );
  });

  it('should keep today when a future time is selected', () => {
    vi.useFakeTimers();

    vi.setSystemTime(
      new Date(
        2026,
        8,
        23,
        12,
        0,
        0,
      ),
    );

    render();

    component.startEditing();

    component.editForm.patchValue({
      date:
        '2026-09-23',

      time:
        '14:00',
    });

    component.onEditTimeChange();

    expect(
      component.editForm.controls
        .date.value,
    ).toBe(
      '2026-09-23',
    );
  });

  it('should reject a past opening datetime', () => {
    render();

    component.startEditing();

    component.editForm.patchValue({
      date:
        '2000-01-01',

      time:
        '12:00',
    });

    component.editForm
      .updateValueAndValidity();

    expect(
      component.editForm
        .hasError(
          'notFuture',
        ),
    ).toBe(true);

    component.saveChanges();

    expect(
      capsuleServiceSpy
        .updateCapsule,
    ).not.toHaveBeenCalled();
  });

  it('should update a collecting capsule with valid values', () => {
    render();

    component.startEditing();

    const futureDate =
      new Date();

    futureDate.setDate(
      futureDate.getDate() + 1,
    );

    const date =
      toDateInputValue(
        futureDate,
      );

    const time =
      '15:30';

    component.editForm.patchValue({
      title:
        'Updated capsule',

      description:
        'Updated description',

      date,

      time,

      timezone:
        'Europe/Madrid',
    });

    const updatedCapsule:
      CapsuleDetail = {
        ...collectingOwner,

        title:
          'Updated capsule',

        description:
          'Updated description',

        opensAt:
          new Date(
            `${date}T${time}`,
          ).toISOString(),
      };

    capsuleServiceSpy
      .updateCapsule
      .mockReturnValue(
        of(
          updatedCapsule,
        ),
      );

    component.saveChanges();

    expect(
      capsuleServiceSpy
        .updateCapsule,
    ).toHaveBeenCalledWith(
      'capsule-123',
      {
        title:
          'Updated capsule',

        description:
          'Updated description',

        opensAt:
          new Date(
            `${date}T${time}`,
          ).toISOString(),

        timezone:
          'Europe/Madrid',
      },
    );

    expect(
      component.capsule()
        ?.title,
    ).toBe(
      'Updated capsule',
    );

    expect(
      component.isEditing(),
    ).toBe(false);
  });

  it('should keep the modal open when update fails', () => {
    render();

    component.startEditing();

    const futureDate =
      new Date();

    futureDate.setDate(
      futureDate.getDate() + 1,
    );

    component.editForm.patchValue({
      title:
        'Updated capsule',

      date:
        toDateInputValue(
          futureDate,
        ),

      time:
        '15:30',

      timezone:
        'Europe/Madrid',
    });

    capsuleServiceSpy
      .updateCapsule
      .mockReturnValue(
        throwError(
          () =>
            new Error(
              'Update failed',
            ),
        ),
      );

    component.saveChanges();

    expect(
      component.isEditing(),
    ).toBe(true);

    expect(
      component.saveError(),
    ).toContain(
      'No se han podido guardar',
    );
  });

  it('should navigate back to the dashboard', () => {
    render();

    component.backToDashboard();

    expect(
      routerSpy.navigate,
    ).toHaveBeenCalledWith([
      '/dashboard',
    ]);
  });
});