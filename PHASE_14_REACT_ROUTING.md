# Phase 14 — React Routing

## Public Routes

/login
/register

## Protected Routes

/user
/manager
/admin

## Route Protection

ProtectedRoute will:

1. Check whether the user is authenticated.
2. If not authenticated, redirect to /login.
3. If authenticated, check the user's role.
4. Allow access only to the appropriate role route.

## Role Mapping

/user     → ROLE_USER
/manager  → ROLE_MANAGER
/admin    → ROLE_ADMIN

## Important Security Rule

React route protection is only a frontend navigation/UX mechanism.

It is NOT the security boundary.

Spring Security must independently protect:

/api/user/**
/api/manager/**
/api/admin/**

A user bypassing React must still receive 401/403 from the backend.